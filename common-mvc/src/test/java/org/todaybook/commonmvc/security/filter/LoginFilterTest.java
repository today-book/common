package org.todaybook.commonmvc.security.filter;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.todaybook.commonmvc.security.AuthenticatedUser;
import org.todaybook.commonmvc.security.Role;

@ExtendWith(MockitoExtension.class)
class LoginFilterTest {
  private LoginFilter loginFilter;

  @BeforeEach
  void setUp() {
    loginFilter = new LoginFilter();
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("필수 헤더가 없으면 인증되지 않음")
  void unauthorizedIfEmptyHeader() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    loginFilter.doFilter(request, response, new MockFilterChain());

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  @DisplayName("헤더가 있으면 Authentication 저장됨")
  void authorized() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-User-Id", 12345);
    request.addHeader("X-Nickname", "김지원");
    request.addHeader("X-User-Roles", "ROLE_ADMIN,ROLE_USER");

    loginFilter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    assertThat(auth).isNotNull();
    assertThat(auth.getPrincipal()).isInstanceOf(AuthenticatedUser.class);

    AuthenticatedUser user = (AuthenticatedUser) auth.getPrincipal();
    assertThat(user.userId()).isEqualTo(12345L);
    assertThat(user.nickname()).isEqualTo("김지원");
    assertThat(user.roles()).isEqualTo(Set.of(Role.USER, Role.ADMIN));
  }
}
