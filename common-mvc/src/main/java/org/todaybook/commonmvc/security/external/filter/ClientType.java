package org.todaybook.commonmvc.security.external.filter;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

/**
 * 요청 주체의 유형을 나타내는 열거형입니다.
 *
 * <p>Gateway에서 전달되는 {@code X-Client-Type} 헤더 값을 기준으로 요청의 성격을 식별하기 위해 사용됩니다.
 *
 * <p>각 타입은 인증 처리 방식의 분기 기준으로 활용되며, 실제 사용자 인증 여부(USER)와 공개 API 접근(PUBLIC)을 명확히 구분합니다.
 *
 * <ul>
 *   <li>{@link #PUBLIC} : 사용자 개념이 없는 공개 API 요청
 *   <li>{@link #USER} : 인증된 사용자 요청
 * </ul>
 *
 * @author 김지원
 * @since 1.1.0
 */
enum ClientType {

  /** 사용자 개념이 없는 공개 API 요청을 의미합니다. */
  PUBLIC,

  /** 사용자 식별 정보가 포함된 인증 요청을 의미합니다. */
  USER;

  /**
   * 문자열 값을 {@link ClientType}으로 변환합니다.
   *
   * <p>Gateway에서 전달된 Header 값을 기반으로 enum 변환을 수행하며, 지원하지 않는 값이 들어오는 경우 인증 정보가 신뢰할 수 없다고 판단하여 {@link
   * AuthenticationCredentialsNotFoundException}을 발생시킵니다.
   *
   * @param value 요청 헤더에서 전달된 ClientType 문자열 값
   * @return 변환된 {@link ClientType}
   * @throws AuthenticationCredentialsNotFoundException 변환할 수 없는 값인 경우
   */
  static ClientType from(String value) {
    try {
      return ClientType.valueOf(value);
    } catch (Exception e) {
      throw new AuthenticationCredentialsNotFoundException("Invalid client type");
    }
  }
}
