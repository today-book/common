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

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

  private final ObjectMapper objectMapper;
  private final MessageResolver messageResolver;

  public void write(HttpServletResponse response, GlobalErrorCode errorCode) throws IOException {

    response.setStatus(errorCode.getStatus());
    response.setContentType("application/json;charset=UTF-8");

    ErrorResponse<Void> body =
        ErrorResponse.of(errorCode.getCode(), messageResolver.resolve(errorCode.getCode()));
    objectMapper.writeValue(response.getWriter(), body);
  }
}
