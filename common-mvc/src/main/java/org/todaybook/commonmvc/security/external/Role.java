package org.todaybook.commonmvc.security.external;

/**
 * 애플리케이션에서 사용하는 사용자 권한(Role)을 정의하는 열거형.
 *
 * <p>외부 시스템 또는 HTTP Header 등에서 전달된 문자열 값을 내부 Role 타입으로 변환하는 기능을 제공합니다.
 *
 * @author 김지원
 * @since 1.1.0
 */
public enum Role {

  /** 일반 사용자 권한. */
  USER,

  /** 관리자 권한. */
  ADMIN;

  /**
   * 문자열 값을 {@link Role} 타입으로 변환합니다.
   *
   * <p>입력 값은 대소문자를 구분하지 않으며, 내부적으로 {@link String#toUpperCase()}를 통해 정규화됩니다.
   *
   * @param value Role 이름 문자열
   * @return 변환된 {@link Role}
   * @throws IllegalArgumentException value에 해당하는 Role이 존재하지 않는 경우
   */
  public static Role from(String value) {
    return Role.valueOf(value.toUpperCase());
  }
}
