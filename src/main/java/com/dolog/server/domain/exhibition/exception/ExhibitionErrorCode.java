package com.dolog.server.domain.exhibition.exception;

import com.dolog.server.global.response.code.BaseResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.dolog.server.global.constant.StaticValue.*;

@Getter
@AllArgsConstructor
public enum ExhibitionErrorCode implements BaseResponseCode {

    EXHIBITION_NOT_FOUND("EXHIBITION_404_1", NOT_FOUND, "전시회를 찾을 수 없습니다."),
    EXHIBITION_UNAUTHORIZED("EXHIBITION_401_1", UNAUTHORIZED, "전시회 등록 권한이 없습니다. 로그인이 필요합니다.");

    private final String code;
    private final int httpStatus;
    private final String message;
}
