package org.todaybook.commonmvc.autoconfig;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.todaybook.commoncore.message.MessageResolver;
import org.todaybook.commonmvc.message.MessageSourceResolver;

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
