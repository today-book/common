package org.todaybook.commonmvc.autoconfig.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "todaybook.security")
public record TodayBookSecurityProperties(Mvc mvc) {
  public record Mvc(boolean enabled) {}
}
