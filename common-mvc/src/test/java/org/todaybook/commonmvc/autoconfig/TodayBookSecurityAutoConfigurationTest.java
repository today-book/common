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
import org.todaybook.commoncore.message.MessageResolver;
import org.todaybook.commonmvc.autoconfig.security.TodayBookSecurityAutoConfiguration;
import org.todaybook.commonmvc.message.MessageSourceResolver;
import org.todaybook.commonmvc.security.SecurityErrorResponseWriter;
import org.todaybook.commonmvc.security.external.filter.LoginFilter;

/**
 * TodayBookSecurityAutoConfiguration Test.
 *
 * <p>Spring Boot Auto-Configuration 테스트 가이드에 따라 조건부 Bean 등록 및 백오프 동작만 검증한다.
 *
 * @author 김지원
 * @since 1.0.0
 */
class TodayBookSecurityAutoConfigurationTest {

  private WebApplicationContextRunner contextRunner;

  @BeforeEach
  void setup() {
    contextRunner =
        new WebApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    SecurityAutoConfiguration.class, TodayBookSecurityAutoConfiguration.class))
            .withUserConfiguration(TestInfraConfig.class)
            .withPropertyValues("todaybook.security.mvc.enabled=true");
  }

  @Test
  @DisplayName("enabled=true 이면 SecurityFilterChain과 LoginFilter가 자동 등록된다")
  void registersSecurityBeans_whenEnabled() {
    contextRunner.run(
        context -> {
          assertThat(context).hasSingleBean(LoginFilter.class);
          assertThat(context).hasSingleBean(SecurityFilterChain.class);
          assertThat(context).hasBean("todayBookSecurityFilterChain");
        });
  }

  @Test
  @DisplayName("enabled=false 이면 TodayBook Security 자동 구성은 적용되지 않는다")
  void doesNotRegisterSecurityBeans_whenDisabled() {
    new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(TodayBookSecurityAutoConfiguration.class))
        .withUserConfiguration(TestInfraConfig.class)
        .withPropertyValues("todaybook.security.mvc.enabled=false")
        .run(
            context -> {
              assertThat(context).doesNotHaveBean(LoginFilter.class);
              assertThat(context).doesNotHaveBean(SecurityFilterChain.class);
            });
  }

  @Test
  @DisplayName("Property가 없으면 TodayBook Security 자동 구성은 적용되지 않는다")
  void doesNotRegisterSecurityBeans_withoutProperty() {
    new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(TodayBookSecurityAutoConfiguration.class))
        .withUserConfiguration(TestInfraConfig.class)
        .run(
            context -> {
              assertThat(context).doesNotHaveBean(LoginFilter.class);
              assertThat(context).doesNotHaveBean(SecurityFilterChain.class);
            });
  }

  @Test
  @DisplayName("사용자가 SecurityFilterChain을 정의하면 자동 구성은 백오프된다")
  void backsOff_whenCustomSecurityFilterChainExists() {
    contextRunner
        .withBean(SecurityFilterChain.class, CustomSecurityFilterChain::new)
        .run(
            context -> {
              assertThat(context).hasSingleBean(SecurityFilterChain.class);
              assertThat(context).doesNotHaveBean("todayBookSecurityFilterChain");
            });
  }

  @Test
  @DisplayName("사용자가 LoginFilter를 정의하면 기본 LoginFilter는 등록되지 않는다")
  void backsOffDefaultLoginFilter_whenCustomLoginFilterExists() {
    contextRunner
        .withBean(LoginFilter.class, CustomLoginFilter::new)
        .run(
            context -> {
              assertThat(context).hasSingleBean(LoginFilter.class);
              assertThat(context.getBean(LoginFilter.class)).isInstanceOf(CustomLoginFilter.class);

              assertThat(context).hasBean("todayBookSecurityFilterChain");
            });
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

    @Bean
    MessageResolver messageResolver() {
      return new MessageSourceResolver(messageSource());
    }

    @Bean
    SecurityErrorResponseWriter securityErrorResponseWriter() {
      return new SecurityErrorResponseWriter(objectMapper(), messageResolver());
    }
  }
}
