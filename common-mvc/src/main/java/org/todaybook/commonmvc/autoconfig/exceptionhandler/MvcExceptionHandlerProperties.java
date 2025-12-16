package org.todaybook.commonmvc.autoconfig.exceptionhandler;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "todaybook.exception")
public record MvcExceptionHandlerProperties(Mvc mvc) {
  public record Mvc(boolean enabled) {}
}
