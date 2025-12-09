package org.todaybook.commonmvc.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

class HeaderAuthenticatedTokenTest {

  @Test
  @DisplayName("authenticated()는 인증 완료 상태의 토큰을 생성한다")
  void authenticatedToken() {
    HeaderAuthenticatedToken token =
        HeaderAuthenticatedToken.authenticated(
            new AuthenticatedUser(1L, "user", Set.of(Role.USER)));

    assertThat(token.isAuthenticated()).isTrue();
    assertThat(token.getCredentials()).isNull();
  }

  @Test
  @DisplayName("authenticated()는 전달된 principal을 그대로 보관한다")
  void principalIsPreserved() {
    AuthenticatedUser principal = new AuthenticatedUser(1L, "user", Set.of(Role.USER));

    HeaderAuthenticatedToken token = HeaderAuthenticatedToken.authenticated(principal);

    assertThat(token.getPrincipal()).isSameAs(principal);
  }

  @Test
  @DisplayName("Role은 ROLE_ 접두사가 붙은 GrantedAuthority로 변환된다")
  void roleToAuthorityMapping() {
    HeaderAuthenticatedToken token =
        HeaderAuthenticatedToken.authenticated(
            new AuthenticatedUser(1L, "user", Set.of(Role.USER)));

    assertThat(token.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly("ROLE_USER");
  }
}
