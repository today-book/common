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
 * TodayBook 공통 MVC 보안 설정을 자동으로 등록하는 Auto-Configuration 클래스다.
 *
 * <h2>이 클래스의 책임</h2>
 *
 * <p>이 클래스는 <b>보안 정책을 정의하지 않는다</b>. 대신 다음 책임만을 가진다.
 *
 * <ul>
 *   <li>공통 SecurityFilterChain을 언제, 어떤 조건에서 등록할지 결정
 *   <li>여러 SecurityFilterChain 간의 실행 순서(Order) 조정
 *   <li>기본 구현(LoginFilter 등)을 제공하되, 사용자 정의 구현이 있으면 즉시 양보
 * </ul>
 *
 * <p>실제 보안 정책(인증 방식, 인가 규칙, 예외 처리 로직)은 {@link BaseSecurityConfig}에 위임한다.
 *
 * <h2>설계 원칙</h2>
 *
 * <ul>
 *   <li>전역(anyRequest) 보안을 제공하지 않는다
 *   <li>API 성격의 요청(/api, /public, /internal)만 대상으로 한다
 *   <li>Spring Boot 기본 Security 설정과 공존 가능해야 한다
 * </ul>
 *
 * <p>이를 통해 공통 라이브러리가 애플리케이션 보안 구성을 침범하지 않도록 한다.
 *
 * @author 김지원
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
   * LoginFilter를 지연 조회하기 위한 Provider다.
   *
   * <p>공통 라이브러리는 기본 LoginFilter를 제공하지만, 사용자가 직접 LoginFilter를 정의한 경우 그 구현을 우선 사용해야 한다.
   *
   * <p>{@link ObjectProvider}를 사용함으로써
   *
   * <ul>
   *   <li>순환 참조를 방지하고
   *   <li>Auto-Configuration 초기화 순서에 의존하지 않으며
   *   <li>실제 사용 시점에 어떤 구현을 쓸지 결정
   * </ul>
   *
   * 할 수 있다.
   */
  private final ObjectProvider<LoginFilter> loginFilterProvider;

  /**
   * 공통 보안 자동 설정 생성자다.
   *
   * <p>{@link SecurityErrorResponseWriter}는 인증/인가 실패 응답을 공통 포맷으로 통일하기 위해 상위 설정에 전달된다.
   */
  public TodayBookSecurityAutoConfiguration(
      SecurityErrorResponseWriter errorResponseWriter,
      ObjectProvider<LoginFilter> loginFilterProvider) {
    super(errorResponseWriter);
    this.loginFilterProvider = loginFilterProvider;
  }

  /**
   * 기본 {@link LoginFilter} 구현을 제공한다.
   *
   * <p>이 빈은 "기본값" 역할만 한다. 사용자가 LoginFilter를 직접 정의하면 즉시 대체된다.
   *
   * <p>공통 라이브러리는 인증 방식의 주도권을 절대 가져가지 않는다.
   */
  @Bean
  @ConditionalOnMissingBean(LoginFilter.class)
  public LoginFilter loginFilter() {
    return new LoginFilter();
  }

  /**
   * {@link BaseSecurityConfig}가 요구하는 인증 필터를 제공한다.
   *
   * <p>실제로 어떤 LoginFilter가 선택될지는
   *
   * <ul>
   *   <li>사용자 정의 LoginFilter
   *   <li>기본 LoginFilter
   * </ul>
   *
   * 중 하나이며, 이 선택은 런타임에 결정된다.
   */
  @Override
  protected LoginFilter loginFilterBean() {
    return loginFilterProvider.getObject();
  }

  /**
   * TodayBook API 영역에 적용될 SecurityFilterChain을 등록한다.
   *
   * <p>이 체인은 다음 요청에만 적용된다.
   *
   * <ul>
   *   <li>/api/**
   *   <li>/public/**
   *   <li>/internal/**
   * </ul>
   *
   * <h3>Order(0)를 사용하는 이유</h3>
   *
   * <p>Spring Boot 기본 SecurityFilterChain은 anyRequest(/**)를 매칭한다. 이 체인이 먼저 평가되면, 본 체인은 절대 실행되지 않는다.
   *
   * <p>따라서 API 체인은 반드시 기본 체인보다 먼저 평가되어야 한다.
   */
  @Bean(name = "todayBookSecurityFilterChain")
  @Order(0)
  @ConditionalOnBean(LoginFilter.class)
  @ConditionalOnMissingBean(name = "todayBookSecurityFilterChain")
  public SecurityFilterChain todayBookSecurityFilterChain(HttpSecurity http) throws Exception {
    return configureSecurityFilterChain(http);
  }

  /**
   * Swagger/OpenAPI 문서 접근을 위한 전용 SecurityFilterChain이다.
   *
   * <p>문서 엔드포인트는 인증/인가 대상이 아니며, LoginFilter나 다른 인증 필터가 절대 개입해서는 안 된다.
   *
   * <p>따라서 별도의 체인으로 분리하여,
   *
   * <ul>
   *   <li>CSRF, CORS 비활성화
   *   <li>모든 요청 permitAll
   * </ul>
   *
   * 로 단순하게 처리한다.
   *
   * <p>Order(1)을 사용해 API 체인 다음에 평가되도록 한다.
   */
  @Bean(name = "todayBookDocsSecurityFilterChain")
  @Order(1)
  @ConditionalOnMissingBean(name = "todayBookDocsSecurityFilterChain")
  public SecurityFilterChain todayBookDocsSecurityFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher(docsSecurityMatcher())
        .csrf(CsrfConfigurer::disable)
        .cors(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(reg -> reg.anyRequest().permitAll());
    return http.build();
  }

  /**
   * 문서 요청에만 매칭되는 RequestMatcher를 생성한다.
   *
   * <p>이 matcher에 매칭되는 요청은 API 보안 체인으로 절대 전달되지 않는다.
   */
  private RequestMatcher docsSecurityMatcher() {
    return new OrRequestMatcher(docsPermitAllRequestMatchers());
  }

  /**
   * 인증 없이 접근 가능한 문서 엔드포인트 목록이다.
   *
   * <p>필요 시 향후 확장(예: Redoc 등)을 고려해 배열 형태로 유지한다.
   */
  private RequestMatcher[] docsPermitAllRequestMatchers() {
    return new RequestMatcher[] {
      PathPatternRequestMatcher.withDefaults().matcher("/v3/api-docs/**"),
      PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui/**"),
      PathPatternRequestMatcher.withDefaults().matcher("/swagger-ui.html")
    };
  }
}
