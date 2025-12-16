package org.todaybook.commonmvc.autoconfig.message;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticMessageSource;
import org.todaybook.commoncore.message.MessageResolver;
import org.todaybook.commonmvc.message.MessageSourceResolver;

/** MessageResolverAutoConfiguration Test. */
class MessageResolverAutoConfigurationTest {

  private WebApplicationContextRunner contextRunner;

  @BeforeEach
  void setup() {
    contextRunner =
        new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MessageResolverAutoConfiguration.class))
            .withUserConfiguration(TestInfraConfig.class);
  }

  @Test
  @DisplayName("MessageSource가 존재하면 MessageResolver가 자동 등록된다")
  void registersMessageResolver_whenMessageSourceExists() {
    contextRunner.run(
        context -> {
          assertThat(context).hasSingleBean(MessageResolver.class);
          assertThat(context.getBean(MessageResolver.class))
              .isInstanceOf(MessageSourceResolver.class);
        });
  }

  @Test
  @DisplayName("사용자가 MessageResolver를 직접 정의하면 기본 MessageSourceResolver는 등록되지 않는다")
  void backsOffWhenCustomMessageResolverExists() {
    contextRunner
        .withUserConfiguration(CustomMessageResolverConfig.class)
        .run(
            context -> {
              assertThat(context).hasSingleBean(MessageResolver.class);
              assertThat(context.getBean(MessageResolver.class))
                  .isInstanceOf(CustomMessageResolver.class);
            });
  }

  @Configuration
  static class TestInfraConfig {

    @Bean
    MessageSource messageSource() {
      return new StaticMessageSource();
    }
  }

  @Configuration
  static class CustomMessageResolverConfig {

    @Bean
    MessageResolver messageResolver() {
      return new CustomMessageResolver();
    }
  }

  static class CustomMessageResolver implements MessageResolver {
    @Override
    public String resolve(String code, Object... args) {
      return "custom";
    }
  }
}
