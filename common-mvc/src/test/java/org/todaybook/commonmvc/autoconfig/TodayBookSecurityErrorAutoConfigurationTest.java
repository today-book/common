package org.todaybook.commonmvc.autoconfig;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticMessageSource;
import org.todaybook.commonmvc.security.SecurityErrorResponseWriter;

class TodayBookSecurityErrorAutoConfigurationTest {

  private WebApplicationContextRunner contextRunner;

  @BeforeEach
  void setup() {
    contextRunner =
        new WebApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    MessageResolverAutoConfiguration.class,
                    TodayBookSecurityErrorAutoConfiguration.class))
            .withPropertyValues("todaybook.security.mvc.enabled=true")
            .withUserConfiguration(TestInfraConfig.class);
  }

  @Test
  @DisplayName("enabled=true 이면 SecurityErrorResponseWriter가 자동 등록된다")
  void registersSecurityErrorResponseWriter_whenEnabled() {
    contextRunner.run(
        context -> {
          assertThat(context).hasSingleBean(SecurityErrorResponseWriter.class);
        });
  }

  @Test
  @DisplayName("security.mvc.enabled 설정이 없으면 SecurityErrorResponseWriter는 등록되지 않는다")
  void doesNotRegisterSecurityErrorResponseWriter_whenPropertyMissing() {
    contextRunner =
        new WebApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    MessageResolverAutoConfiguration.class,
                    TodayBookSecurityErrorAutoConfiguration.class))
            .withUserConfiguration(TestInfraConfig.class);

    contextRunner.run(
        context -> {
          assertThat(context).doesNotHaveBean(SecurityErrorResponseWriter.class);
        });
  }

  @Configuration
  static class TestInfraConfig {

    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper();
    }

    @Bean
    MessageSource messageSource() {
      return new StaticMessageSource();
    }
  }
}
