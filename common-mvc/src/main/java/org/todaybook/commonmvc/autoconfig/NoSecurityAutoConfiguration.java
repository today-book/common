package org.todaybook.commonmvc.autoconfig;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security 기능이 비활성화된 경우(No-Op), 모든 요청을 허용하는 SecurityFilterChain을 제공하는 Auto-Configuration.
 *
 * <p>이 설정은 {@code todaybook.security.mvc.enabled=false} 인 경우에 활성화되며, Spring Boot 기본
 * SecurityAutoConfiguration이 등록하는 기본 SecurityFilterChain을 명시적으로 대체하기 위해 사용됩니다.
 *
 * <p>본 설정이 활성화되면:
 *
 * <ul>
 *   <li>인증 없이 모든 요청 허용
 *   <li>CSRF 비활성화
 *   <li>Form Login / HTTP Basic 비활성화
 * </ul>
 *
 * @author 김지원
 * @since 1.1.0
 */
@AutoConfiguration(before = SecurityAutoConfiguration.class)
@ConditionalOnClass(SecurityFilterChain.class)
@ConditionalOnProperty(
    prefix = "todaybook.security.mvc",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class NoSecurityAutoConfiguration {

  /**
   * 모든 요청을 허용하는 No-Op {@link SecurityFilterChain}을 등록합니다.
   *
   * <p>Spring Boot 기본 보안 체인이 등록되기 전에 우선 적용되어, 보안 기능이 완전히 비활성화된 상태를 보장합니다.
   *
   * @param http {@link HttpSecurity} 보안 구성 객체
   * @return 인증이 비활성화된 {@link SecurityFilterChain}
   * @throws Exception 보안 구성 중 발생할 수 있는 예외
   */
  @Bean(name = "noOpSecurityFilterChain")
  public SecurityFilterChain noOpSecurityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable);

    return http.build();
  }
}
