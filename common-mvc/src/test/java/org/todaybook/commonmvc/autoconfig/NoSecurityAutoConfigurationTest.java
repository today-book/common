package org.todaybook.commonmvc.autoconfig;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.todaybook.commonmvc.autoconfig.security.NoSecurityAutoConfiguration;

class NoSecurityAutoConfigurationTest {

  private WebApplicationContextRunner contextRunner;

  @BeforeEach
  void setUp() {
    contextRunner =
        new WebApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    NoSecurityAutoConfiguration.class, SecurityAutoConfiguration.class));
  }

  @Test
  @DisplayName("enabled=false 이면 No-Op SecurityFilterChain이 등록된다")
  void registersNoOpSecurityFilterChain_whenSecurityDisabled() {
    contextRunner
        .withPropertyValues("todaybook.security.mvc.enabled=false")
        .run(
            context -> {
              assertThat(context).hasBean("noOpSecurityFilterChain");
            });
  }

  @Test
  @DisplayName("enabled=true 이면 No-Op SecurityFilterChain이 등록되지 않는다")
  void doesNotRegisterNoOpSecurityFilterChain_whenSecurityEnabled() {
    contextRunner
        .withPropertyValues("todaybook.security.mvc.enabled=true")
        .run(
            context -> {
              assertThat(context).doesNotHaveBean("noOpSecurityFilterChain");
            });
  }

  @Test
  @DisplayName("보안 설정 값이 없으면 No-Op SecurityFilterChain이 기본으로 등록된다")
  void registersNoOpSecurityFilterChain_whenPropertyIsMissing() {
    contextRunner.run(
        context -> {
          assertThat(context).hasBean("noOpSecurityFilterChain");
        });
  }
}
