package org.todaybook.commonmvc.security.external;

/**
 * Spring Security 권한(Authority) 관련 상수를 정의하는 유틸리티 클래스입니다.
 *
 * <p>이 클래스는 Spring Security에서 사용하는 권한 문자열의 접두사(prefix)를 공통 상수로 관리하기 위해 제공됩니다.
 *
 * <p>기본적으로 Spring Security는 {@code hasRole("USER")}와 같은 표현에서 내부적으로 {@code ROLE_} 접두사를 자동으로 처리하므로,
 * 실제 {@code GrantedAuthority} 값은 {@code ROLE_USER} 형식을 따릅니다.
 *
 * <p>권한 문자열 생성 시 하드 코딩을 방지하고, 역할(Role)과 보안 프레임워크(GrantedAuthority) 간의 경계를 명확히 하기 위해 해당 상수를 제공합니다.
 *
 * <p>이 클래스는 인스턴스화될 필요가 없으므로 {@code final}로 선언되며, 모든 멤버는 정적(static) 상수로만 구성됩니다.
 *
 * @author 김지원
 * @since 1.0.0
 */
public final class AuthorityConstants {

  /**
   * Spring Security 권한 문자열의 기본 접두사입니다.
   *
   * <p>예: {@code ROLE_USER}, {@code ROLE_ADMIN}
   */
  public static final String ROLE_PREFIX = "ROLE_";

  private AuthorityConstants() {
    // 유틸리티 클래스이므로 인스턴스 생성을 방지합니다.
  }
}
