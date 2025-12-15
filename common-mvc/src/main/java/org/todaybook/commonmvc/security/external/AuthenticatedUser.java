package org.todaybook.commonmvc.security.external;

import java.util.Set;
import java.util.UUID;

/**
 * 인증이 완료된 사용자를 표현하는 Security 도메인용 Principal 객체입니다.
 *
 * <p>이 클래스는 Gateway, OAuth 서버, 인증 서버 등 외부 인증 시스템을 통해 이미 인증이 완료된 사용자의 최소 정보를 애플리케이션 내부로 전달하기 위한 목적으로
 * 사용됩니다.
 *
 * <p>Spring Security의 {@code UserDetails}를 직접 구현하지 않으며, 보안 프레임워크에 종속되지 않는 순수 도메인 모델을 유지하는 것을 목표로
 * 합니다.
 *
 * <p>주로 {@link org.springframework.security.core.Authentication#getPrincipal()}을 통해 컨트롤러 및 서비스 계층에서
 * 현재 사용자 정보를 조회하는 데 사용됩니다.
 *
 * <p>이 Principal은 권한 정보를 {@link Role} 단위로 보유하며, 실제 {@code GrantedAuthority} 변환 및 인가 판단은 Security
 * 계층에서 수행됩니다.
 *
 * <p>GUEST 사용자의 경우 사용자 식별 정보는 존재하지 않지만, Gateway에 의해 신뢰된 요청으로 판단되어 Spring Security 관점에서는
 * 인증된(authenticated) 상태로 취급됩니다.
 *
 * @param userId 인증된 사용자의 고유 식별자 (GUEST 사용자의 경우 {@code null})
 * @param nickname 사용자 닉네임 또는 표시 이름
 * @param roles 사용자에게 부여된 역할(Role) 집합
 * @author 김지원
 * @since 1.0.0
 */
public record AuthenticatedUser(UUID userId, String nickname, Set<Role> roles) {

  /**
   * Compact Constructor.
   *
   * <p>roles가 {@code null}로 전달되는 경우를 방지하고, 외부에서 전달된 컬렉션을 불변(Set.copyOf)으로 변환하여 Principal 객체의 불변성과
   * 안정성을 보장합니다.
   *
   * @param userId 사용자 식별자
   * @param nickname 사용자 닉네임
   * @param roles 사용자 권한 집합
   */
  public AuthenticatedUser {
    roles = roles != null ? Set.copyOf(roles) : Set.of();
  }

  /**
   * PUBLIC(비로그인) 사용자를 생성합니다.
   *
   * <p>PUBLIC 클라이언트 또는 인증이 필요 없는 요청에 사용되며, 사용자 ID와 닉네임은 {@code null}이지만, {@link Role#PUBLIC} 권한을 가진
   * 인증된 사용자로 취급됩니다.
   *
   * @return PUBLIC 사용자 Principal
   */
  public static AuthenticatedUser publicClient() {
    return new AuthenticatedUser(null, null, Set.of(Role.PUBLIC));
  }
}
