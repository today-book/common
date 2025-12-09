package org.todaybook.commonmvc.security;

import java.util.Set;

/**
 * 인증이 완료된 사용자를 표현하는 Security 도메인용 Principal 객체입니다.
 *
 * <p>이 클래스는 외부 인증 시스템(Gateway, OAuth 서버, 인증 서버 등)을 통해 이미 인증된 사용자의 최소 정보를 애플리케이션 내부로 전달하기 위한 용도로
 * 사용됩니다.
 *
 * <p>Spring Security의 {@code UserDetails}를 직접 구현하지 않으며, 보안 프레임워크로부터 독립적인 순수 도메인 모델을 유지하는 것을 목표로
 * 합니다.
 *
 * <p>주로 {@link org.springframework.security.core.Authentication#getPrincipal()}을 통해 컨트롤러, 서비스 계층에서
 * 현재 사용자 정보를 조회하는 데 사용됩니다.
 *
 * <p>이 Principal은 권한 정보를 {@link Role} 단위로 보유하며, 실제 {@code GrantedAuthority} 변환은 Security 계층에서
 * 수행됩니다.
 *
 * @param userId 인증된 사용자의 고유 식별자
 * @param nickname 사용자 닉네임 또는 표시 이름
 * @param roles 사용자에게 부여된 역할(Role) 집합
 * @author 김지원
 * @since 1.0.0
 */
public record AuthenticatedUser(Long userId, String nickname, Set<Role> roles) {}
