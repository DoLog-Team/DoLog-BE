package com.dolog.server.global.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.dolog.server.global.exception.BaseException;
import com.dolog.server.global.response.BaseResponse;
import com.dolog.server.global.response.ErrorResponse;
import jakarta.servlet.http.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException)
            throws IOException {

        // 필터에서 설정한 예외 객체 가져오기 (JwtAuthenticationFilter에서 setAttribute로 전달 가능)
        Exception ex = (Exception) request.getAttribute("jwt_exception");

        ErrorResponse<?> errorResponse;

        // BaseException (JwtExpiredException, JwtInvalidException 등) 인 경우
        if (ex instanceof BaseException baseEx) {
            errorResponse = ErrorResponse.of(
                    baseEx.getErrorCode().getCode(),
                    baseEx.getMessage(),
                    baseEx.getErrorCode().getHttpStatus()
            );
            response.setStatus(baseEx.getErrorCode().getHttpStatus());
        }
        // 토큰 누락, 형식 불일치 등 일반적인 Authentication 예외
        else {
            errorResponse = ErrorResponse.of(
                    "JWT_401_UNAUTHORIZED",
                    "인증이 필요합니다. (토큰이 없거나 잘못되었습니다.)",
                    HttpStatus.UNAUTHORIZED.value()
            );
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }

        // JSON 형태로 클라이언트에 응답
        response.setContentType("application/json; charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
