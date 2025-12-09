package org.todaybook.commonmvc.autoconfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.todaybook.commoncore.message.MessageResolver;
import org.todaybook.commonmvc.security.SecurityErrorResponseWriter;

@AutoConfiguration
@ConditionalOnClass(SecurityErrorResponseWriter.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class TodayBookSecurityErrorAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(SecurityErrorResponseWriter.class)
  public SecurityErrorResponseWriter securityErrorResponseWriter(
      ObjectMapper objectMapper, MessageResolver messageResolver) {
    return new SecurityErrorResponseWriter(objectMapper, messageResolver);
  }
}
