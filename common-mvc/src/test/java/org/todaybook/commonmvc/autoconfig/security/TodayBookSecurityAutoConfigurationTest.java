package org.todaybook.commonmvc.autoconfig.security;

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
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.todaybook.commoncore.message.MessageResolver;
import org.todaybook.commonmvc.message.MessageSourceResolver;
import org.todaybook.commonmvc.security.SecurityErrorResponseWriter;
import org.todaybook.commonmvc.security.external.filter.LoginFilter;

/**
 * TodayBookSecurityAutoConfiguration 테스트다.
 *
 * <p>목표는 "보안 정책 자체"가 아니라, Auto-Configuration의 조건부 빈 등록과 백오프 동작만 검증하는 것이다.
 *
 * <p>테스트에서 Spring Boot의 SecurityAutoConfiguration을 올리면 defaultSecurityFilterChain(/**)이 함께 생성되어 우리
 * 체인과 충돌(또는 unreachable chain 검증 실패)할 수 있으므로, Boot 자동 구성은 제외하고 Spring Security 최소 인프라만 올린다.
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
            .withConfiguration(AutoConfigurations.of(TodayBookSecurityAutoConfiguration.class))
            // HttpSecurity 주입/체인 빌드를 위한 최소한의 Spring Security 인프라만 제공한다.
            .withUserConfiguration(TestSecurityInfraConfig.class, TestInfraConfig.class);
  }

  @Test
  @DisplayName("enabled=true 이면 LoginFilter와 TodayBook SecurityFilterChain들이 등록된다")
  void registersSecurityBeans_whenEnabled() {
    contextRunner
        .withPropertyValues("todaybook.security.mvc.enabled=true")
        .run(
            context -> {
              assertThat(context).hasSingleBean(LoginFilter.class);
              assertThat(context).hasBean("todayBookSecurityFilterChain");
              assertThat(context).hasBean("todayBookDocsSecurityFilterChain");
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
              assertThat(context).doesNotHaveBean("todayBookDocsSecurityFilterChain");
            });
  }

  @Test
  @DisplayName("Property가 없으면 TodayBook Security 자동 구성은 적용되지 않는다")
  void doesNotRegisterSecurityBeans_withoutProperty() {
    contextRunner.run(
        context -> {
          assertThat(context).doesNotHaveBean(LoginFilter.class);
          assertThat(context).doesNotHaveBean("todayBookSecurityFilterChain");
          assertThat(context).doesNotHaveBean("todayBookDocsSecurityFilterChain");
        });
  }

  @Test
  @DisplayName("사용자가 LoginFilter를 정의하면 기본 LoginFilter는 백오프된다")
  void backsOffDefaultLoginFilter_whenCustomLoginFilterExists() {
    contextRunner
        .withPropertyValues("todaybook.security.mvc.enabled=true")
        .withUserConfiguration(CustomLoginFilterConfig.class)
        .run(
            context -> {
              assertThat(context).hasSingleBean(LoginFilter.class);
              assertThat(context.getBean(LoginFilter.class)).isInstanceOf(CustomLoginFilter.class);
              assertThat(context).hasBean("todayBookSecurityFilterChain");
              assertThat(context).hasBean("todayBookDocsSecurityFilterChain");
            });
  }

  static class CustomLoginFilter extends LoginFilter {}

  /**
   * Spring Security의 HttpSecurity/FilterChainProxy 구성을 위한 최소 인프라다.
   *
   * <p>Boot의 SecurityAutoConfiguration(defaultSecurityFilterChain)을 끌어오지 않기 위해
   * 테스트에서는 @EnableWebSecurity만 사용한다.
   */
  @Configuration
  @EnableWebSecurity
  static class TestSecurityInfraConfig {}

  @Configuration
  static class CustomLoginFilterConfig {
    @Bean
    LoginFilter loginFilter() {
      return new CustomLoginFilter();
    }
  }

  /**
   * TodayBookSecurityAutoConfiguration이 필요로 하는 최소 의존 빈들이다.
   *
   * <p>이 테스트의 관심사는 "조건부 등록"이므로, 보안 정책 검증에 불필요한 인프라는 추가하지 않는다.
   */
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
    MessageResolver messageResolver(MessageSource messageSource) {
      return new MessageSourceResolver(messageSource);
    }

    @Bean
    SecurityErrorResponseWriter securityErrorResponseWriter(
        ObjectMapper objectMapper, MessageResolver messageResolver) {
      return new SecurityErrorResponseWriter(objectMapper, messageResolver);
    }
  }
}
