package org.todaybook.commonmvc.security.external;

import static org.todaybook.commonmvc.security.external.AuthorityConstants.ROLE_PREFIX;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Header 기반 인증 방식에서 사용되는 Authentication 구현체입니다.
 *
 * <p>이 토큰은 Gateway 또는 외부 인증 시스템을 통해 이미 인증이 완료된 사용자를 Spring Security의 {@link
 * org.springframework.security.core.context.SecurityContext}에 설정하기 위한 용도로 사용됩니다.
 *
 * <p>본 토큰은 비밀번호 기반 인증을 수행하지 않으며, 생성 시점에 항상 인증 완료 상태({@code isAuthenticated = true})로 설정됩니다. 따라서 인증
 * 요청(authentication request)을 표현하지 않고, 오직 <b>인증 결과(authentication result)</b>만을 표현합니다.
 *
 * <p>사용자 정보는 {@link AuthenticatedUser} 형태로 보관되며, 권한 정보는 {@link Role}을 기반으로 {@link
 * GrantedAuthority}로 변환됩니다.
 *
 * @author 김지원
 * @since 1.0.0
 */
@Getter
public class HeaderAuthenticatedToken extends AbstractAuthenticationToken {

  /** 인증이 완료된 사용자 Principal 객체. */
  private final AuthenticatedUser principal;

  /**
   * {@link AuthenticatedUser}을 기반으로 인증 완료 토큰을 생성합니다.
   *
   * <p>생성자 내부에서 Role 정보를 {@link GrantedAuthority}로 변환하며, 인증 완료 상태로 설정됩니다.
   *
   * @param principal 인증된 사용자 정보
   */
  private HeaderAuthenticatedToken(AuthenticatedUser principal) {
    super(toAuthorities(principal.roles()));
    setAuthenticated(true);
    this.principal = principal;
  }

  /**
   * 인증 완료된 {@link HeaderAuthenticatedToken}을 생성하는 정적 팩토리 메서드입니다.
   *
   * <p>외부에서는 이 메서드를 통해서만 토큰을 생성할 수 있으며, 이를 통해 항상 인증 완료 상태의 토큰만 생성하도록 보장합니다.
   *
   * @param principal 인증된 사용자 정보
   * @return 인증 완료 상태의 {@link HeaderAuthenticatedToken}
   */
  public static HeaderAuthenticatedToken authenticated(AuthenticatedUser principal) {
    return new HeaderAuthenticatedToken(principal);
  }

  /**
   * 인증 자격 증명(credentials)을 반환합니다.
   *
   * <p>Header 기반 Pre-authentication 구조에서는 비밀번호나 토큰 자체를 보관하지 않으므로 {@code null}을 반환합니다.
   *
   * @return 항상 {@code null}
   */
  @Override
  public Object getCredentials() {
    return null;
  }

  /**
   * 인증된 사용자의 Principal 객체를 반환합니다.
   *
   * <p>컨트롤러나 서비스 계층에서는 {@code @AuthenticationPrincipal}을 통해 해당 객체에 접근할 수 있습니다.
   *
   * @return {@link AuthenticatedUser}
   */
  @Override
  public Object getPrincipal() {
    return principal;
  }

  /**
   * {@link Role} 집합을 Spring Security에서 사용하는 {@link GrantedAuthority} 컬렉션으로 변환합니다.
   *
   * <p>각 Role은 {@code ROLE_} 접두사가 붙은 권한 문자열로 변환됩니다. 예: {@code Role.USER -> ROLE_USER}
   *
   * @param roles 사용자에게 부여된 역할 집합
   * @return 변환된 {@link GrantedAuthority} 목록
   */
  private static Collection<? extends GrantedAuthority> toAuthorities(Set<Role> roles) {
    if (roles == null || roles.isEmpty()) {
      return List.of();
    }
    return roles.stream()
        .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.name()))
        .toList();
  }
}
