package org.todaybook.commonmvc.autoconfig;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.security.web.SecurityFilterChain;
import org.todaybook.commonmvc.security.external.filter.LoginFilter;

class TodayBookSecurityAutoConfigurationTest {

  private WebApplicationContextRunner contextRunner;

  @BeforeEach
  void setup() {
    contextRunner =
        new WebApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    SecurityAutoConfiguration.class,
                    TodayBookSecurityAutoConfiguration.class,
                    MessageResolverAutoConfiguration.class,
                    TodayBookSecurityErrorAutoConfiguration.class))
            .withUserConfiguration(TestInfraConfig.class)
            .withPropertyValues("todaybook.security.mvc.enabled=true");
  }

  @Test
  @DisplayName("enabled=true 이면 TodayBook SecurityFilterChain과 LoginFilter가 자동 등록된다")
  void registersSecurityFilterChainAndLoginFilter_whenEnabled() {
    contextRunner.run(
        context -> {
          assertThat(context).hasSingleBean(LoginFilter.class);
          assertThat(context).hasBean("todayBookSecurityFilterChain");
        });
  }

  @Test
  @DisplayName("PropertyValues가 없으면 TodayBook Security 자동 구성은 적용되지 않는다")
  void doesNotRegisterSecurityBeans_withoutPropertyValues() {
    contextRunner =
        new WebApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    TodayBookSecurityAutoConfiguration.class, SecurityAutoConfiguration.class));
    contextRunner.run(
        context -> {
          assertThat(context).doesNotHaveBean(LoginFilter.class);
          assertThat(context).doesNotHaveBean("todayBookSecurityFilterChain");
        });
  }

  @Test
  @DisplayName("enabled=false 이면 TodayBook Security 자동 구성은 적용되지 않는다")
  void doesNotRegisterSecurityBeans_whenDisabled() {
    contextRunner
        .withPropertyValues("todaybook.security.mvc.enabled=false")
        .run(
            context -> {
              assertThat(context).doesNotHaveBean(LoginFilter.class);
              assertThat(context).doesNotHaveBean("todayBookSecurityFilterChain");
            });
  }

  @Test
  @DisplayName("사용자가 SecurityFilterChain을 정의하면 TodayBook 자동 구성은 백오프된다")
  void backsOffAutoConfiguration_whenCustomSecurityFilterChainExists() {
    contextRunner
        .withUserConfiguration(CustomSecurityFilterChainConfig.class)
        .run(
            context -> {
              assertThat(context).hasSingleBean(SecurityFilterChain.class);
              assertThat(context.getBean(SecurityFilterChain.class))
                  .isInstanceOf(CustomSecurityFilterChain.class);

              assertThat(context).doesNotHaveBean("todayBookSecurityFilterChain");
            });
  }

  @Test
  @DisplayName("사용자가 LoginFilter를 정의하면 기본 LoginFilter는 자동 등록되지 않는다")
  void backsOffDefaultLoginFilter_whenCustomLoginFilterExists() {
    contextRunner
        .withUserConfiguration(CustomLoginFilterConfig.class)
        .run(
            context -> {
              assertThat(context).hasSingleBean(LoginFilter.class);
              assertThat(context.getBean(LoginFilter.class)).isInstanceOf(CustomLoginFilter.class);

              // SecurityFilterChain은 여전히 TodayBook 자동 구성으로 등록됨
              assertThat(context).hasBean("todayBookSecurityFilterChain");
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
