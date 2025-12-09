package org.todaybook.commonmvc.security.external.filter;

import static org.todaybook.commonmvc.security.external.AuthorityConstants.ROLE_PREFIX;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
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
 * <p>본 필터는 이미 인증이 완료된 요청(Header 기반 Pre-authentication)을 애플리케이션 내부에서 Spring Security
 * Authentication으로 변환하는 역할만 수행하며, 실제 인증(로그인) 행위는 수행하지 않습니다.
 *
 * <p>다음과 같은 헤더 정보를 사용합니다.
 *
 * <ul>
 *   <li>{@code X-User-Id} : 사용자 식별자
 *   <li>{@code X-Nickname} : 사용자 닉네임
 *   <li>{@code X-User-Roles} : 사용자 권한 목록 (comma separated)
 * </ul>
 *
 * <p>이미 SecurityContext에 인증 정보가 존재하는 경우, 기존 인증을 덮어쓰지 않도록 필터 처리를 건너뜁니다.
 *
 * @author 김지원
 * @since 1.0.0
 */
public class LoginFilter extends OncePerRequestFilter {

  /** 사용자 ID 헤더 키. */
  private static final String HEADER_USER_ID = "X-User-Id";

  /** 사용자 닉네임 헤더 키. */
  private static final String HEADER_NICKNAME = "X-Nickname";

  /** 사용자 권한 목록 헤더 키. */
  private static final String HEADER_ROLES = "X-User-Roles";

  /**
   * 요청당 한 번 실행되는 필터 로직입니다.
   *
   * <p>이미 인증된 요청인 경우 인증 로직을 생략하고, 그렇지 않은 경우 Header 기반 인증 시도를 수행합니다.
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

    // 이미 인증된 요청인 경우 SecurityContext를 덮어쓰지 않음
    if (isAlreadyAuthenticated()) {
      filterChain.doFilter(request, response);
      return;
    }

    authenticateIfPossible(request);
    filterChain.doFilter(request, response);
  }

  /**
   * 요청 헤더에 인증 정보가 존재하는 경우 인증을 수행합니다.
   *
   * <p>{@code X-User-Id} 헤더가 존재하지 않으면 인증 시도를 하지 않습니다.
   *
   * @param request 현재 HTTP 요청
   */
  private void authenticateIfPossible(HttpServletRequest request) {
    Long userId = parseUserId(request.getHeader(HEADER_USER_ID));
    if (userId == null) {
      return;
    }

    String nickname = request.getHeader(HEADER_NICKNAME);
    Set<Role> roles = parseRoles(request.getHeader(HEADER_ROLES));

    AuthenticatedUser principal = new AuthenticatedUser(userId, nickname, roles);

    SecurityContextHolder.getContext()
        .setAuthentication(HeaderAuthenticatedToken.authenticated(principal));
  }

  /**
   * 현재 요청이 이미 인증된 상태인지 확인합니다.
   *
   * <p>다른 인증 필터(JWT, OAuth 등)에 의해 이미 인증된 경우, 본 필터에서 인증 정보를 덮어쓰지 않기 위해 사용됩니다.
   *
   * @return 인증이 이미 완료된 경우 {@code true}
   */
  private boolean isAlreadyAuthenticated() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.isAuthenticated();
  }

  /**
   * 사용자 ID 헤더 값을 {@link Long} 타입으로 변환합니다.
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
   * 권한 문자열을 {@link Role} 집합으로 변환합니다.
   *
   * <p>권한 값은 {@code ROLE_USER, ROLE_ADMIN} 과 같은 형식을 허용하며, {@code ROLE_} 접두사는 제거한 후 {@link Role}
   * enum으로 변환합니다. 변환할 수 없는 값은 무시됩니다.
   *
   * @param roles 권한 문자열
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
   * @return 접두사 제거된 권한 문자열
   */
  private String removeRolePrefix(String role) {
    return role.startsWith(ROLE_PREFIX) ? role.substring(ROLE_PREFIX.length()) : role;
  }
}
