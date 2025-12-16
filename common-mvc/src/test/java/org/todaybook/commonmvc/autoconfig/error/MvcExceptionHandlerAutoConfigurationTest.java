package org.todaybook.commonmvc.autoconfig.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.todaybook.commoncore.message.MessageResolver;
import org.todaybook.commonmvc.autoconfig.exceptionhandler.MvcExceptionHandlerAutoConfiguration;
import org.todaybook.commonmvc.error.MvcExceptionHandler;

public class MvcExceptionHandlerAutoConfigurationTest {

  private WebApplicationContextRunner contextRunner;

  @BeforeEach
  void setup() {
    contextRunner =
        new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MvcExceptionHandlerAutoConfiguration.class))
            .withUserConfiguration(TestInfraConfig.class);
  }

  @Test
  @DisplayName("기본 조건 충족 시 MvcExceptionHandler가 자동 등록된다")
  void defaultAutoConfigurationWorks() {
    contextRunner.run(
        context -> {
          assertThat(context).hasSingleBean(MvcExceptionHandler.class);
        });
  }

  @Test
  @DisplayName("enabled=false 이면 MvcExceptionHandler가 등록되지 않는다")
  void disabledProperty() {
    contextRunner
        .withPropertyValues("todaybook.exception.mvc.enabled=false")
        .run(
            context -> {
              assertThat(context).doesNotHaveBean(MvcExceptionHandler.class);
            });
  }

  @Test
  @DisplayName("사용자가 직접 MvcExceptionHandler 빈을 정의하면 자동 등록되지 않는다")
  void customMvcExceptionHandlerBeanExists() {
    contextRunner
        .withUserConfiguration(CustomHandlerConfig.class)
        .run(
            context -> {
              assertThat(context).hasSingleBean(MvcExceptionHandler.class);
              assertThat(context.getBean(MvcExceptionHandler.class))
                  .isInstanceOf(CustomHandler.class);
            });
  }

  @Configuration
  static class CustomHandlerConfig {
    @Bean
    public MvcExceptionHandler mvcExceptionHandler() {
      return new CustomHandler();
    }
  }

  static class CustomHandler extends MvcExceptionHandler {
    public CustomHandler() {
      super(null);
    }
  }

  @Configuration
  static class TestInfraConfig {

    @Bean
    MessageResolver messageResolver() {
      return (code, args) -> code;
    }
  }
}
