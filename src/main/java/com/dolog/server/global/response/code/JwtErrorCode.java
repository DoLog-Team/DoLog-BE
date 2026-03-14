package com.dolog.server.global.response.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.dolog.server.global.constant.StaticValue.*;

@Getter
@AllArgsConstructor
public enum JwtErrorCode implements BaseResponseCode {

    // 인증 관련
    JWT_401_EXPIRED("JWT_401_EXPIRED", UNAUTHORIZED, "JWT 토큰이 만료되었습니다."),
    JWT_401_INVALID("JWT_401_INVALID", UNAUTHORIZED, "유효하지 않은 JWT 토큰입니다."),
    JWT_401_UNSUPPORTED("JWT_401_UNSUPPORTED", UNAUTHORIZED, "지원되지 않는 JWT 토큰 형식입니다."),
    JWT_401_MALFORMED("JWT_401_MALFORMED", UNAUTHORIZED, "JWT 구조가 올바르지 않습니다.");

    private final String code;
    private final int httpStatus;
    private final String message;
}
