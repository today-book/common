package org.todaybook.commonmvc.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.todaybook.commonmvc.error.GlobalErrorCode;
import org.todaybook.commonmvc.security.external.filter.LoginFilter;

/**
 * 공통 Spring Security 설정을 제공하는 기반 클래스입니다.
 *
 * <p>이 클래스의 목적은 “모든 요청을 통제”하는 것이 아니라, <b>공통으로 반복되는 보안 구성의 기준선(baseline)을 제공</b>하는 데 있습니다.
 *
 * <p>실제 인증 방식(JWT, 세션, 헤더 등)이나 접근 정책의 세부 내용은 서비스마다 다를 수 있으므로, 이 클래스는 다음 책임만 가집니다.
 *
 * <ul>
 *   <li>보안 필터 체인이 적용될 요청 범위 정의
 *   <li>Stateless API 환경에 적합한 기본 Security 옵션 구성
 *   <li>커스텀 {@link LoginFilter}를 안전한 위치에 삽입
 *   <li>401 / 403 응답을 공통 포맷으로 일관되게 처리
 * </ul>
 *
 * <p>정책 변경이 필요한 지점은 protected 메서드로 분리하여, 하위 클래스에서 의도를 명확히 드러내며 확장할 수 있도록 설계되어 있습니다.
 *
 * <p><b>중요:</b> 이 클래스는 공통 라이브러리 용도로 사용되므로, {@code securityMatcher("/**")} 처럼 전체 요청을 잡지 않고 명시적으로 API
 * 성격의 경로만 대상으로 삼습니다.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BaseSecurityConfig {

  /**
   * 이 SecurityFilterChain이 적용될 요청 범위를 정의합니다.
   *
   * <p>공통 라이브러리가 모든 요청을 가로채면 애플리케이션에서 정의한 추가 SecurityFilterChain과 충돌하기 쉽기 때문에, 실제 인증/인가가 필요한 API
   * 성격의 경로만 대상으로 제한합니다.
   *
   * <p>필요 시 하위 클래스에서 이 메서드를 오버라이드하여 적용 범위를 명확하게 조정할 수 있습니다.
   */
  protected RequestMatcher apiSecurityMatcher() {
    return new OrRequestMatcher(
        PathPatternRequestMatcher.withDefaults().matcher("/api/**"),
        PathPatternRequestMatcher.withDefaults().matcher("/public/**"),
        PathPatternRequestMatcher.withDefaults().matcher("/internal/**"));
  }

  /**
   * 인증 처리를 담당하는 커스텀 {@link LoginFilter}를 반환합니다.
   *
   * <p>공통 설정에서는 “어떤 인증을 한다”보다 “인증 필터를 언제 실행할 것인가”만 책임집니다.
   *
   * <p>따라서 실제 인증 로직(JWT 검증, 사용자 조회 등)은 반드시 하위 클래스에서 구현하도록 강제합니다.
   *
   * <p>반환된 필터는 {@link UsernamePasswordAuthenticationFilter} 이전에 등록되며, 인가 단계 전에 인증 컨텍스트를 구성하는 역할을
   * 합니다.
   */
  protected abstract LoginFilter loginFilterBean();

  /**
   * 인증/인가 실패 시 공통 에러 응답을 작성하는 전용 writer입니다.
   *
   * <p>401 / 403 응답의 포맷이 서비스마다 달라지면 프론트엔드 또는 다른 서비스에서의 공통 처리 로직이 깨질 수 있으므로, 공통 라이브러리 레벨에서 응답 형식을
   * 고정합니다.
   */
  private final SecurityErrorResponseWriter errorResponseWriter;

  /**
   * 공통 SecurityFilterChain을 구성합니다.
   *
   * <p>이 체인은 다음 전제를 기반으로 설계되었습니다.
   *
   * <ul>
   *   <li>REST API 환경이며 서버 세션을 사용하지 않는다
   *   <li>인증은 커스텀 {@link LoginFilter}에서 처리한다
   *   <li>기본 보안 옵션은 모든 서비스에서 동일해야 한다
   * </ul>
   *
   * <p>보안 정책의 “내용”이 아닌, “구조와 실행 순서”를 고정하는 역할을 합니다.
   */
  public SecurityFilterChain configureSecurityFilterChain(HttpSecurity http) throws Exception {
    LoginFilter loginFilter = loginFilterBean();
    Assert.notNull(loginFilter, "LoginFilter bean is required but was not provided.");

    http.securityMatcher(apiSecurityMatcher())
        .csrf(CsrfConfigurer::disable)
        .cors(AbstractHttpConfigurer::disable)
        .addFilterBefore(loginFilter, UsernamePasswordAuthenticationFilter.class)
        .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(authorizeHttpRequests())
        .exceptionHandling(exceptionHandlingCustomizer());

    return http.build();
  }

  /**
   * 인가(Authorization) 정책의 기본 구현입니다.
   *
   * <p>기본 정책은 다음과 같습니다.
   *
   * <ul>
   *   <li>내부 호출용 엔드포인트는 인증을 생략한다
   *   <li>그 외 모든 요청은 인증이 필요하다
   * </ul>
   *
   * <p>내부 호출은 보통 VPC, Gateway, 네트워크 레벨에서 보호된다는 전제를 두고 단순화합니다.
   *
   * <p>단, 이 정책은 보안 요구사항에 따라 하위 클래스에서 반드시 재정의할 수 있도록 열어둡니다.
   */
  protected Customizer<
          AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry>
      authorizeHttpRequests() {

    return registry ->
        registry
            .requestMatchers(permitAllRequestMatchers())
            .permitAll()
            .anyRequest()
            .authenticated();
  }

  /**
   * 인증 없이 접근을 허용할 경로 목록을 정의합니다.
   *
   * <p>이 메서드는 “인가 단계”에서의 예외 규칙만 담당합니다. 실제로 인증 필터가 이 경로를 스킵하는지는 {@link LoginFilter#shouldNotFilter}
   * 구현에 의해 보장되어야 합니다.
   *
   * <p>즉, 이 메서드와 LoginFilter의 스킵 로직은 함께 동작할 때 내부 호출이 안전하게 보장됩니다.
   */
  protected RequestMatcher[] permitAllRequestMatchers() {
    return new RequestMatcher[] {PathPatternRequestMatcher.withDefaults().matcher("/internal/**")};
  }

  /**
   * 인증/인가 예외 처리 구성을 반환합니다.
   *
   * <p>이 지점을 분리한 이유는, 서비스별로 로깅 전략이나 응답 정책을 선택적으로 바꾸고 싶을 수 있기 때문입니다.
   */
  protected Customizer<ExceptionHandlingConfigurer<HttpSecurity>> exceptionHandlingCustomizer() {
    return handler -> {
      handler.authenticationEntryPoint(authenticationEntryPoint());
      handler.accessDeniedHandler(accessDeniedHandler());
    };
  }

  /**
   * 인증되지 않은 요청(401)에 대한 공통 처리 로직입니다.
   *
   * <p>보안 예외는 로그에는 남기되, 클라이언트에는 내부 구현이 노출되지 않도록 표준 에러 코드만 반환합니다.
   */
  protected AuthenticationEntryPoint authenticationEntryPoint() {
    return (req, res, e) -> {
      log.warn("UNAUTHORIZED {}: {}", e.getClass().getSimpleName(), e.getMessage());
      errorResponseWriter.write(res, GlobalErrorCode.UNAUTHORIZED);
    };
  }

  /**
   * 인증은 되었으나 권한이 부족한 경우(403)에 대한 처리입니다.
   *
   * <p>권한 정책은 서비스별로 달라질 수 있으므로, 응답 포맷만 공통으로 고정하고 세부 정책은 상위 레이어에서 관리하도록 합니다.
   */
  protected AccessDeniedHandler accessDeniedHandler() {
    return (req, res, e) -> {
      log.warn("FORBIDDEN {}: {}", e.getClass().getSimpleName(), e.getMessage());
      errorResponseWriter.write(res, GlobalErrorCode.FORBIDDEN);
    };
  }
}
