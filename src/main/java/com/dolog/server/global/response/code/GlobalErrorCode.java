package com.dolog.server.global.response.code;

import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.dolog.server.global.constant.StaticValue.*;

@Getter
@AllArgsConstructor
public enum GlobalErrorCode implements BaseResponseCode {

    BAD_REQUEST_ERROR("GLOBAL_400_1", BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_HTTP_MESSAGE_BODY("GLOBAL_400_2", BAD_REQUEST, "HTTP 요청 바디의 형식이 잘못되었습니다."),
    GLOBAL_INVALID_ENUM("GLOBAL_400_3", BAD_REQUEST,"잘못된 ENUM 값입니다."),
    ACCESS_DENIED_REQUEST("GLOBAL_403", FORBIDDEN, "해당 요청에 접근 권한이 없습니다."),
    UNSUPPORTED_HTTP_METHOD("GLOBAL_405", METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    CONFLICT_ERROR("GLOBAL_409", CONFLICT, "데이터 중복 또는 충돌이 발생했습니다."),
    SERVER_ERROR( "GLOBAL_500", INTERNAL_SERVER_ERROR, "서버 내부에서 알 수 없는 오류가 발생했습니다.");

    private final String code;
    private final int httpStatus;
    private final String message;
}
