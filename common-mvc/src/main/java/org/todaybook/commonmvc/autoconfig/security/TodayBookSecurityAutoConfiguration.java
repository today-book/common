package org.todaybook.commonmvc.autoconfig.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.todaybook.commonmvc.security.BaseSecurityConfig;
import org.todaybook.commonmvc.security.SecurityErrorResponseWriter;
import org.todaybook.commonmvc.security.external.filter.LoginFilter;

/**
 * TodayBook Spring MVC 환경을 위한 Spring Security 자동 설정 클래스.
 *
 * <p>이 AutoConfiguration은 다음 조건을 만족할 때 활성화된다:
 *
 * <ul>
 *   <li>Servlet 기반 Spring MVC 애플리케이션
 *   <li>Spring Security가 classpath에 존재
 *   <li>{@code todaybook.security.mvc.enabled=true} 설정이 명시된 경우
 * </ul>
 *
 * <p>본 설정은 {@link SecurityAutoConfiguration} 이후에 적용되며, TodayBook 서비스에 필요한 보안 구성을 추가로 확장한다.
 *
 * <p>구성 내용:
 *
 * <ul>
 *   <li>API 요청을 처리하는 기본 {@link SecurityFilterChain}
 *   <li>Swagger / OpenAPI 문서 접근을 위한 PermitAll 전용 {@link SecurityFilterChain}
 *   <li>{@link LoginFilter}를 확장 포인트로 제공
 * </ul>
 */
@AutoConfiguration(after = SecurityAutoConfiguration.class)
@EnableMethodSecurity
@ConditionalOnClass(SecurityFilterChain.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(TodayBookSecurityProperties.class)
@ConditionalOnProperty(
    prefix = "todaybook.security.mvc",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false)
public class TodayBookSecurityAutoConfiguration extends BaseSecurityConfig {

  /**
   * {@link LoginFilter}를 지연 조회하기 위한 {@link ObjectProvider}.
   *
   * <p>사용자가 직접 {@link LoginFilter} Bean을 정의한 경우, 해당 Bean을 우선적으로 사용하기 위해 Provider를 사용한다.
   */
  private final ObjectProvider<LoginFilter> loginFilterProvider;

  /**
   * TodayBook Security 자동 설정 생성자.
   *
   * @param errorResponseWriter 인증/인가 실패 시 공통 에러 응답을 작성하는 컴포넌트
   * @param loginFilterProvider {@link LoginFilter} 확장 포인트 Provider
   */
  public TodayBookSecurityAutoConfiguration(
      SecurityErrorResponseWriter errorResponseWriter,
      ObjectProvider<LoginFilter> loginFilterProvider) {
    super(errorResponseWriter);
    this.loginFilterProvider = loginFilterProvider;
  }

  /**
   * 기본 {@link LoginFilter} Bean을 등록한다.
   *
   * <p>사용자가 {@link LoginFilter}를 직접 정의하지 않은 경우에만 기본 구현체가 자동으로 등록된다.
   *
   * @return 기본 {@link LoginFilter} 인스턴스
   */
  @Bean
  @ConditionalOnMissingBean(LoginFilter.class)
  public LoginFilter loginFilter() {
    return new LoginFilter();
  }

  /**
   * {@link BaseSecurityConfig}에서 사용할 {@link LoginFilter}를 제공한다.
   *
   * <p>사용자 정의 {@link LoginFilter}가 존재하면 해당 Bean을 반환하며, 없을 경우 기본 {@link LoginFilter}가 사용된다.
   *
   * @return 사용 가능한 {@link LoginFilter} (없을 경우 {@code null})
   */
  @Override
  protected LoginFilter loginFilterBean() {
    return loginFilterProvider.getIfAvailable();
  }

  /**
   * TodayBook API 요청을 처리하는 기본 {@link SecurityFilterChain}.
   *
   * <p>대부분의 API 요청을 처리하는 보안 체인으로, {@link LoginFilter}가 존재하는 경우에만 등록된다.
   *
   * <p>Docs 전용 FilterChain보다 낮은 우선순위({@code @Order(0)})로 적용되어, 문서 요청은 먼저 Docs FilterChain에서 처리된다.
   *
   * @param http {@link HttpSecurity}
   * @return 구성된 API {@link SecurityFilterChain}
   * @throws Exception 보안 설정 중 발생할 수 있는 예외
   */
  @Bean(name = "todayBookSecurityFilterChain")
  @Order(0)
  @ConditionalOnBean(LoginFilter.class)
  @ConditionalOnMissingBean(name = "todayBookSecurityFilterChain")
  public SecurityFilterChain todayBookSecurityFilterChain(HttpSecurity http) throws Exception {
    return configureSecurityFilterChain(http);
  }

  /**
   * Swagger 및 OpenAPI 문서 접근을 위한 PermitAll {@link SecurityFilterChain}.
   *
   * <p>문서 관련 요청은 인증/인가 없이 접근 가능해야 하므로, API 보안 FilterChain보다 높은 우선순위({@code @Order(-1)})로 먼저 매칭되도록
   * 구성된다.
   *
   * <p>이 FilterChain은 문서 요청에만 적용되며, 실제 API 요청에는 영향을 주지 않는다.
   *
   * @param http {@link HttpSecurity}
   * @return Docs 전용 {@link SecurityFilterChain}
   * @throws Exception 보안 설정 중 발생할 수 있는 예외
   */
  @Bean(name = "todayBookDocsSecurityFilterChain")
  @Order(-1)
  @ConditionalOnMissingBean(name = "todayBookDocsSecurityFilterChain")
  public SecurityFilterChain todayBookDocsSecurityFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher(docsSecurityMatcher())
        .csrf(CsrfConfigurer::disable)
        .cors(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(reg -> reg.anyRequest().permitAll());
    return http.build();
  }

  /**
   * Swagger / OpenAPI 문서 요청에 매칭되는 {@link RequestMatcher}를 생성한다.
   *
   * @return Docs 요청 전용 {@link RequestMatcher}
   */
  private RequestMatcher docsSecurityMatcher() {
    return new OrRequestMatcher(docsPermitAllRequestMatchers());
  }

  /**
   * 인증 없이 접근을 허용할 문서 관련 URL 패턴 목록.
   *
   * @return Docs PermitAll {@link RequestMatcher} 배열
   */
  private RequestMatcher[] docsPermitAllRequestMatchers() {
    return new RequestMatcher[] {
      PathPatternRequestMatcher.withDefaults().matcher("/v3/api-docs/**"),
      PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui/**"),
      PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui.html")
    };
  }
}
