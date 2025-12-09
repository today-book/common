package org.todaybook.commonmvc.autoconfig;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;
import org.todaybook.commonmvc.security.external.filter.LoginFilter;

class DefaultSecurityAutoConfigurationTest {

  private WebApplicationContextRunner contextRunner;

  @BeforeEach
  void setup() {
    contextRunner =
        new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DefaultSecurityAutoConfiguration.class));
  }

  @Test
  @DisplayName("기본 조건 충족 시 LoginFilter & SecurityFilterChain 자동 등록된다")
  void defaultAutoConfigurationWorks() {
    contextRunner.run(
        context -> {
          assertThat(context).hasSingleBean(LoginFilter.class);
          assertThat(context).hasSingleBean(SecurityFilterChain.class);
        });
  }

  @Test
  @DisplayName("enabled=false 이면 LoginFilter & SecurityFilterChain 모두 등록되지 않는다")
  void disabledProperty() {
    contextRunner
        .withPropertyValues("todaybook.security.mvc.enabled=false")
        .run(
            context -> {
              assertThat(context).doesNotHaveBean(LoginFilter.class);
              assertThat(context).doesNotHaveBean(SecurityFilterChain.class);
            });
  }

  @Test
  @DisplayName("사용자가 SecurityFilterChain을 직접 정의하면 자동 등록되지 않는다")
  void customSecurityFilterChainExists() {
    contextRunner
        .withUserConfiguration(CustomSecurityFilterChainConfig.class)
        .run(
            context -> {
              assertThat(context).hasSingleBean(SecurityFilterChain.class);
              assertThat(context.getBean(SecurityFilterChain.class))
                  .isInstanceOf(CustomSecurityFilterChain.class);

              // AutoConfiguration의 defaultSecurityFilterChain은 등록되지 않는다
              assertThat(context)
                  .doesNotHaveBean(
                      DefaultSecurityAutoConfiguration.class.getName()
                          + ".defaultSecurityFilterChain");
            });
  }

  @Test
  @DisplayName("사용자가 LoginFilter를 직접 정의하면 기본 LoginFilter는 등록되지 않는다")
  void customLoginFilterExists() {
    contextRunner
        .withUserConfiguration(CustomLoginFilterConfig.class)
        .run(
            context -> {
              assertThat(context).hasSingleBean(LoginFilter.class);
              assertThat(context.getBean(LoginFilter.class)).isInstanceOf(CustomLoginFilter.class);

              // SecurityFilterChain은 여전히 자동 등록된다
              assertThat(context).hasSingleBean(SecurityFilterChain.class);
            });
  }

  @Configuration
  static class CustomSecurityFilterChainConfig {

    @Bean
    public SecurityFilterChain customChain() {
      return new CustomSecurityFilterChain();
    }
  }

  @Configuration
  static class CustomLoginFilterConfig {

    @Bean
    public LoginFilter loginFilter() {
      return new CustomLoginFilter();
    }
  }

  static class CustomSecurityFilterChain implements SecurityFilterChain {

    @Override
    public boolean matches(HttpServletRequest request) {
      return true;
    }

    @Override
    public List<Filter> getFilters() {
      return List.of();
    }
  }

  static class CustomLoginFilter extends LoginFilter {}
}
