package org.todaybook.commonmvc.autoconfig.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "todaybook.security.mvc")
public class TodayBookSecurityProperties {
  private boolean enabled = false;
}
