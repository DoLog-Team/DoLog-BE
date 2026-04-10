package com.dolog.server.domain.exhibition.exception;

import com.dolog.server.global.response.code.BaseResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.dolog.server.global.constant.StaticValue.*;

@Getter
@AllArgsConstructor
public enum ExhibitionErrorCode implements BaseResponseCode {

    EXHIBITION_NOT_FOUND("EXHIBITION_404_1", NOT_FOUND, "전시회를 찾을 수 없습니다."),
    EXHIBITION_UNAUTHORIZED("EXHIBITION_401_1", UNAUTHORIZED, "전시회 등록 권한이 없습니다. 로그인이 필요합니다."),

    EXHIBITION_MAP_ALREADY_EXISTS("EXHIBITION_MAP_400", BAD_REQUEST, "이미 장소 정보가 등록된 전시회입니다."),
    EXHIBITION_MAP_NOT_FOUND("EXHIBITION_MAP_404", NOT_FOUND, "등록된 장소 정보가 없습니다."),

    EXHIBITION_ARTIST_ALREADY_EXISTS("EXHIBITION_ARTIST_400", BAD_REQUEST, "이미 전시에 추가된 작가입니다."),
    EXHIBITION_ARTIST_NOT_FOUND("EXHIBITION_ARTIST_404", NOT_FOUND, "해당 전시에 등록된 작가가 아닙니다."),

    HOST_NOT_FOUND("EXHIBITION_HOST_404", NOT_FOUND, "등록된 호스트 정보가 없습니다.");

    private final String code;
    private final int httpStatus;
    private final String message;
}
