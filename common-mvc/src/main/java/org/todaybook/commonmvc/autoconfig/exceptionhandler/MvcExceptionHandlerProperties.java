package org.todaybook.commonmvc.autoconfig.exceptionhandler;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "todaybook.exception.mvc")
public class MvcExceptionHandlerProperties {
  private boolean enabled = true;
}
