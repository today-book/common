package org.todaybook.commonmvc.security.external.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.todaybook.commonmvc.security.external.AuthenticatedUser;
import org.todaybook.commonmvc.security.external.Role;

@ExtendWith(MockitoExtension.class)
class LoginFilterTest {
  private LoginFilter loginFilter;

  @BeforeEach
  void setUp() {
    loginFilter = new LoginFilter();
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("Gateway 신뢰 헤더가 없으면 인증 예외 발생")
  void unauthorizedIfNoGatewayTrustedHeader() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    assertThatThrownBy(
            () ->
                loginFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain()))
        .isInstanceOf(AuthenticationCredentialsNotFoundException.class);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  @DisplayName("PUBLIC 요청은 userId 없이 인증됨")
  void authenticatedPublicRequest() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Gateway-Trusted", "true");
    request.addHeader("X-Client-Type", "PUBLIC");

    loginFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    assertThat(auth).isNotNull();
    assertThat(auth.isAuthenticated()).isTrue();

    AuthenticatedUser user = (AuthenticatedUser) auth.getPrincipal();
    assertThat(user.userId()).isNull();
    assertThat(user.nickname()).isNull();
    assertThat(user.roles()).contains(Role.PUBLIC);
  }

  @Test
  @DisplayName("USER 요청은 userId, roles 포함하여 인증됨")
  void authenticatedUserRequest() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Gateway-Trusted", "true");
    request.addHeader("X-Client-Type", "USER");
    request.addHeader("X-User-Id", "12345");
    request.addHeader("X-User-Nickname", "김지원");
    request.addHeader("X-User-Roles", "ROLE_ADMIN,ROLE_USER");

    loginFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    assertThat(auth).isNotNull();

    AuthenticatedUser user = (AuthenticatedUser) auth.getPrincipal();
    assertThat(user.userId()).isEqualTo(12345L);
    assertThat(user.nickname()).isEqualTo("김지원");
    assertThat(user.roles()).isEqualTo(Set.of(Role.USER, Role.ADMIN));
  }

  @Test
  @DisplayName("USER 요청인데 userId가 없으면 인증 실패")
  void failIfUserRequestWithoutUserId() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-Gateway-Trusted", "true");
    request.addHeader("X-Client-Type", "USER");

    assertThatThrownBy(
            () ->
                loginFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain()))
        .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
  }
}
