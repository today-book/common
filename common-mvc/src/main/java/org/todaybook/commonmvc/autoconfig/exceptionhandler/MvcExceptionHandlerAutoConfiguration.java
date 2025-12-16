package org.todaybook.commonmvc.autoconfig.exceptionhandler;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.todaybook.commoncore.message.MessageResolver;
import org.todaybook.commonmvc.error.MvcExceptionHandler;

/**
 * {@code MvcExceptionHandler}를 Spring MVC 환경에 자동으로 등록하는 AutoConfiguration 클래스.
 *
 * <p>이 설정은 다음 조건을 만족할 때만 활성화된다:
 *
 * <ul>
 *   <li>Servlet 기반 Spring MVC 애플리케이션인 경우
 *   <li>{@code todaybook.exception.mvc.enabled=true} 인 경우 (설정이 없으면 기본적으로 활성화됨)
 * </ul>
 *
 * <p>사용자가 직접 {@code MvcExceptionHandler} Bean을 정의한 경우, 해당 Bean을 우선 사용하며 본 자동 설정은 적용되지 않는다.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(MvcExceptionHandlerProperties.class)
@ConditionalOnProperty(
    prefix = "todaybook.exception.mvc",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class MvcExceptionHandlerAutoConfiguration {

  /**
   * 기본 {@link MvcExceptionHandler} Bean을 등록한다.
   *
   * <p>이미 {@link MvcExceptionHandler} 타입의 Bean이 존재하는 경우, 사용자 정의 Bean을 우선하기 위해 본 Bean은 등록되지 않는다.
   *
   * @param messageResolver 에러 메시지 코드 해석 및 다국어 메시지 변환을 담당하는 {@link MessageResolver}
   * @return 기본 {@link MvcExceptionHandler} 인스턴스
   */
  @Bean
  @ConditionalOnMissingBean(MvcExceptionHandler.class)
  public MvcExceptionHandler mvcExceptionHandler(MessageResolver messageResolver) {
    return new MvcExceptionHandler(messageResolver);
  }
}
