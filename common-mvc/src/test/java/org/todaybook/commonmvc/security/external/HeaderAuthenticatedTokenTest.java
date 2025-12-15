package org.todaybook.commonmvc.security.external;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

class HeaderAuthenticatedTokenTest {

  private AuthenticatedUser principal;

  @BeforeEach
  void init() {
    principal =
        new AuthenticatedUser(
            UUID.fromString("3f7c9a2e-9c6e-4b1f-8c2e-6a4c5d9b1e72"), "user", Set.of(Role.USER));
  }

  @Test
  @DisplayName("authenticated()는 인증 완료 상태의 토큰을 생성한다")
  void authenticatedToken() {
    HeaderAuthenticatedToken token = HeaderAuthenticatedToken.authenticated(principal);

    assertThat(token.isAuthenticated()).isTrue();
    assertThat(token.getCredentials()).isNull();
  }

  @Test
  @DisplayName("authenticated()는 전달된 principal을 그대로 보관한다")
  void principalIsPreserved() {
    HeaderAuthenticatedToken token = HeaderAuthenticatedToken.authenticated(principal);

    assertThat(token.getPrincipal()).isSameAs(principal);
  }

  @Test
  @DisplayName("Role은 ROLE_ 접두사가 붙은 GrantedAuthority로 변환된다")
  void roleToAuthorityMapping() {
    HeaderAuthenticatedToken token = HeaderAuthenticatedToken.authenticated(principal);

    assertThat(token.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly("ROLE_USER");
  }
}
