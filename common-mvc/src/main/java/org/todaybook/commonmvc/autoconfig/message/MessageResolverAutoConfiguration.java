package org.todaybook.commonmvc.autoconfig.message;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.todaybook.commoncore.message.MessageResolver;
import org.todaybook.commonmvc.message.MessageSourceResolver;

/**
 * Spring {@link MessageSource} 기반 {@link MessageResolver}를 자동으로 구성하는 Auto-Configuration 클래스.
 *
 * <p>Servlet 기반 WebApplication 환경에서 동작하며, 애플리케이션에 {@link MessageSource}가 존재하는 경우에만 활성화됩니다.
 *
 * <p>이미 사용자 정의 {@link MessageResolver} 빈이 등록되어 있다면 해당 빈이 우선 적용되고, 이 자동 구성은 동작하지 않습니다.
 *
 * @author 김지원
 * @since 1.1.0
 */
@AutoConfiguration
@ConditionalOnClass(MessageSource.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class MessageResolverAutoConfiguration {

  /**
   * {@link MessageSource} 기반 메시지 해석기(MessageResolver)를 등록한다.
   *
   * <p>사용자가 직접 {@link MessageResolver} 빈을 제공하는 경우 이 기본 구현은 적용되지 않는다.
   *
   * @param messageSource Spring MessageSource (messages.properties 등)
   * @return 기본 메시지 해석기 {@link MessageSourceResolver}
   */
  @Bean
  @ConditionalOnMissingBean(MessageResolver.class)
  public MessageResolver messageResolver(MessageSource messageSource) {
    return new MessageSourceResolver(messageSource);
  }
}
