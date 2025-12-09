package org.todaybook.commonmvc.security.external.filter;

import static org.todaybook.commonmvc.security.external.AuthorityConstants.ROLE_PREFIX;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.todaybook.commonmvc.security.external.AuthenticatedUser;
import org.todaybook.commonmvc.security.external.HeaderAuthenticatedToken;
import org.todaybook.commonmvc.security.external.Role;

/**
 * Gateway 또는 외부 인증 시스템에서 전달한 HTTP Header 기반 사용자 정보를 Spring Security Context에 인증된 사용자로 설정하는 필터입니다.
 *
 * <p>본 필터는 Gateway에서 이미 검증된 요청을 전제로 하며, Header 기반 Pre-authentication 정보를 내부 {@link Authentication}
 * 객체로 변환하는 역할만 수행합니다. 실제 로그인, 토큰 검증 등의 인증 행위는 수행하지 않습니다.
 *
 * <p>이미 {@link SecurityContextHolder}에 인증 정보가 존재하는 경우, 기존 인증(JWT, OAuth2 등)을 덮어쓰지 않도록 필터 처리를 생략합니다.
 *
 * @author 김지원
 * @since 1.0.0
 */
public class LoginFilter extends OncePerRequestFilter {

  /** Gateway에서 전달하는 신뢰 여부 헤더 키. */
  private static final String HEADER_GATEWAY_TRUSTED = "X-Gateway-Trusted";

  /** 요청 주체의 클라이언트 타입(USER / PUBLIC)을 나타내는 헤더 키. */
  private static final String HEADER_CLIENT_TYPE = "X-Client-Type";

  /** 사용자 ID 헤더 키. */
  private static final String HEADER_USER_ID = "X-User-Id";

  /** 사용자 닉네임 헤더 키. */
  private static final String HEADER_USER_NICKNAME = "X-User-Nickname";

  /** 사용자 권한 목록 헤더 키 (comma separated). */
  private static final String HEADER_USER_ROLES = "X-User-Roles";

  /**
   * 요청당 한 번 실행되는 필터 진입점입니다.
   *
   * <p>이미 인증된 요청인 경우 인증 로직을 수행하지 않고 그대로 다음 필터로 전달합니다. 인증되지 않은 요청에 대해서만 Gateway Header 기반 인증을 시도합니다.
   *
   * @param request HTTP 요청
   * @param response HTTP 응답
   * @param filterChain 필터 체인
   * @throws ServletException 서블릿 처리 중 예외 발생 시
   * @throws IOException I/O 오류 발생 시
   */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    // 이미 인증된 컨텍스트가 존재하면 덮어쓰지 않음
    if (isAlreadyAuthenticated()) {
      filterChain.doFilter(request, response);
      return;
    }

    authenticateFromGateway(request);
    filterChain.doFilter(request, response);
  }

  /**
   * Gateway에서 전달된 Header 정보를 기반으로 인증 객체를 생성합니다.
   *
   * <p>Gateway 신뢰 여부를 먼저 검증한 뒤, {@link ClientType}에 따라 USER / PUBLIC 인증을 분기 처리합니다.
   *
   * @param request 현재 HTTP 요청
   */
  private void authenticateFromGateway(HttpServletRequest request) {
    validateGatewayTrusted(request);

    ClientType clientType = ClientType.from(request.getHeader(HEADER_CLIENT_TYPE));

    // ClientType에 따라 인증 주체를 명확히 분리
    AuthenticatedUser principal =
        switch (clientType) {
          case USER -> authenticateUser(request);
          case PUBLIC -> AuthenticatedUser.publicClient();
        };

    SecurityContextHolder.getContext()
        .setAuthentication(HeaderAuthenticatedToken.authenticated(principal));
  }

  /**
   * 요청이 Gateway를 통해 전달된 신뢰 가능한 요청인지 검증합니다.
   *
   * <p>Gateway에서 내부 서비스로 전달되는 요청에만 설정되는 헤더를 기준으로 검증하며, 해당 값이 없거나 올바르지 않을 경우 인증 실패로 처리합니다.
   *
   * @param request 현재 HTTP 요청
   */
  private void validateGatewayTrusted(HttpServletRequest request) {
    if (!"true".equals(request.getHeader(HEADER_GATEWAY_TRUSTED))) {
      throw new AuthenticationCredentialsNotFoundException("Gateway authentication failed");
    }
  }

  /**
   * USER 타입 요청에 대해 사용자 인증 정보를 생성합니다.
   *
   * <p>필수 사용자 식별자({@code X-User-Id})가 존재하지 않거나 유효하지 않은 경우 인증 실패로 처리합니다.
   *
   * @param request 현재 HTTP 요청
   * @return 인증된 {@link AuthenticatedUser}
   */
  private AuthenticatedUser authenticateUser(HttpServletRequest request) {
    Long userId = parseUserId(request.getHeader(HEADER_USER_ID));
    if (userId == null) {
      throw new AuthenticationCredentialsNotFoundException(
          "Invalid USER authentication from gateway");
    }

    String nickname = decodeIfPresent(request.getHeader(HEADER_USER_NICKNAME));
    Set<Role> roles = parseRoles(request.getHeader(HEADER_USER_ROLES));

    return new AuthenticatedUser(userId, nickname, roles);
  }

  /**
   * 현재 요청이 이미 인증된 상태인지 확인합니다.
   *
   * @return 인증 객체가 존재하고 인증 완료 상태인 경우 {@code true}
   */
  private boolean isAlreadyAuthenticated() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.isAuthenticated();
  }

  /**
   * 사용자 ID 문자열을 {@link Long} 타입으로 변환합니다.
   *
   * @param userId 사용자 ID 문자열
   * @return 변환된 사용자 ID, 변환 실패 시 {@code null}
   */
  private Long parseUserId(String userId) {
    if (!StringUtils.hasText(userId)) {
      return null;
    }
    try {
      return Long.parseLong(userId);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * Header 값이 존재하는 경우 URL 디코딩을 수행합니다.
   *
   * <p>Gateway에서 UTF-8 기준으로 URL 인코딩된 값을 전달하는 경우를 고려한 처리입니다.
   *
   * @param value 인코딩된 문자열
   * @return 디코딩된 문자열, 값이 없을 경우 {@code null}
   */
  private String decodeIfPresent(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return URLDecoder.decode(value, StandardCharsets.UTF_8);
  }

  /**
   * 권한 문자열을 {@link Role} 집합으로 변환합니다.
   *
   * <p>{@code ROLE_} 접두사를 제거한 뒤 {@link Role} enum으로 안전하게 매핑하며, 변환할 수 없는 값은 무시됩니다.
   *
   * @param roles 권한 문자열 (comma separated)
   * @return 변환된 Role 집합
   */
  private Set<Role> parseRoles(String roles) {
    if (!StringUtils.hasText(roles)) {
      return Set.of();
    }

    return Arrays.stream(roles.split(","))
        .map(String::trim)
        .map(this::removeRolePrefix)
        .map(this::safeToRole)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  /**
   * 문자열을 {@link Role} enum으로 안전하게 변환합니다.
   *
   * @param value 권한 문자열
   * @return 변환된 Role, 실패 시 {@code null}
   */
  private Role safeToRole(String value) {
    try {
      return Role.from(value);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * 권한 문자열에서 {@code ROLE_} 접두사를 제거합니다.
   *
   * @param role 권한 문자열
   * @return 접두사가 제거된 권한 문자열
   */
  private String removeRolePrefix(String role) {
    return role.startsWith(ROLE_PREFIX) ? role.substring(ROLE_PREFIX.length()) : role;
  }
}
