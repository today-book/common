package org.todaybook.commonmvc.autoconfig.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.todaybook.commoncore.message.MessageResolver;
import org.todaybook.commonmvc.security.SecurityErrorResponseWriter;

/**
 * TodayBook MVC 보안 에러 응답 처리를 위한 Auto-Configuration 클래스.
 *
 * <p>Servlet 기반 WebApplication 환경에서 동작하며, TodayBook MVC 보안이 활성화된 경우 {@link
 * SecurityErrorResponseWriter} 빈을 자동으로 등록합니다.
 *
 * <p>애플리케이션에서 동일 타입의 {@link SecurityErrorResponseWriter} 빈을 직접 정의한 경우에는 해당 빈이 우선적으로 사용되며, 이 자동 구성은
 * 적용되지 않습니다.
 *
 * <p>이 구성은 {@link TodayBookSecurityAutoConfiguration} 이후에 적용되어, 기본 보안 설정이 먼저 초기화된 이후 에러 응답 처리 로직을
 * 보완하는 역할을 합니다.
 *
 * @author 김지원
 * @since 1.1.0
 */
@AutoConfiguration(after = TodayBookSecurityAutoConfiguration.class)
@ConditionalOnClass(SecurityErrorResponseWriter.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(
    prefix = "todaybook.security.mvc",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false)
public class TodayBookSecurityErrorAutoConfiguration {

  /**
   * Security 인증/인가 과정에서 발생하는 에러를 공통 응답 포맷으로 작성하는 {@link SecurityErrorResponseWriter} 빈을 생성합니다.
   *
   * <p>JSON 직렬화를 위해 {@link ObjectMapper}를 사용하며, 다국어 메시지 처리를 위해 {@link MessageResolver}를 함께 구성합니다.
   *
   * @param objectMapper JSON 직렬화를 위한 ObjectMapper
   * @param messageResolver 에러 메시지 해석을 위한 메시지 리졸버
   * @return SecurityErrorResponseWriter 인스턴스
   */
  @Bean
  @ConditionalOnMissingBean(SecurityErrorResponseWriter.class)
  public SecurityErrorResponseWriter securityErrorResponseWriter(
      ObjectMapper objectMapper, MessageResolver messageResolver) {
    return new SecurityErrorResponseWriter(objectMapper, messageResolver);
  }
}
