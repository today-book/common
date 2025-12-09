package org.todaybook.commonmvc.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.todaybook.commoncore.error.ErrorResponse;
import org.todaybook.commoncore.message.MessageResolver;
import org.todaybook.commonmvc.error.GlobalErrorCode;

/**
 * Spring Security 인증/인가 오류 발생 시 공통 에러 응답(JSON)을 작성하는 Writer 클래스입니다.
 *
 * <p>이 클래스는 {@link org.springframework.security.web.AuthenticationEntryPoint} 또는 {@link
 * org.springframework.security.web.access.AccessDeniedHandler} 등에서 사용되며, Security 계층에서 발생한 예외를
 * {@link GlobalErrorCode} 기반의 공통 응답 포맷으로 변환합니다.
 *
 * <p>응답 메시지는 {@link MessageResolver}를 통해 메시지 코드 기반으로 해석되며, 클라이언트에는 일관된 {@link ErrorResponse} 형식의
 * JSON 응답이 전달됩니다.
 *
 * <p>ControllerAdvice 영역과 분리하여 Security 전용 오류 응답 책임만을 명확히 갖도록 설계되었습니다.
 *
 * @author 김지원
 * @since 1.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

  /** JSON 직렬화를 위한 Jackson ObjectMapper. */
  private final ObjectMapper objectMapper;

  /** 에러 코드에 대응하는 메시지를 해석하기 위한 MessageResolver. */
  private final MessageResolver messageResolver;

  /**
   * Security 오류 응답을 HTTP Response에 직접 작성합니다.
   *
   * <p>{@link GlobalErrorCode}에 정의된 HTTP 상태 코드와 에러 코드를 사용하여 응답을 구성하며, 메시지는 {@link MessageResolver}를
   * 통해 코드 기반으로 해석됩니다.
   *
   * <p>이 메서드는 Security Filter 체인 내에서 호출되므로, Response Body를 직접 작성하는 책임을 가집니다.
   *
   * @param response HTTP 응답 객체
   * @param errorCode Security 오류에 대응하는 {@link GlobalErrorCode}
   * @throws IOException 응답 작성 중 I/O 오류가 발생한 경우
   */
  public void write(HttpServletResponse response, GlobalErrorCode errorCode) throws IOException {

    response.setStatus(errorCode.getStatus());
    response.setContentType("application/json;charset=UTF-8");

    ErrorResponse<Void> body =
        ErrorResponse.of(errorCode.getCode(), messageResolver.resolve(errorCode.getCode()));

    objectMapper.writeValue(response.getWriter(), body);
  }
}
