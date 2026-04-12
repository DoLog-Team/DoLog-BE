package com.dolog.server.domain.bts.exception;

import com.dolog.server.global.response.code.BaseResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.dolog.server.global.constant.StaticValue.*;

@Getter
@AllArgsConstructor
public enum BtsErrorCode implements BaseResponseCode {

    BTS_NOT_FOUND("BTS_404_1", NOT_FOUND, "BTS를 찾을 수 없습니다."),
    BTS_EXHIBITION_MISMATCH("BTS_400_1", BAD_REQUEST, "해당 전시에 속하지 않는 BTS입니다."),
    ARTIST_PROFILE_EXHIBITION_MISMATCH("BTS_400_2", BAD_REQUEST, "해당 전시에 속하지 않는 작가 프로필입니다."),
    ARTWORK_EXHIBITION_MISMATCH("BTS_400_3", BAD_REQUEST, "해당 전시에 속하지 않는 작품이 포함되어 있습니다.");

    private final String code;
    private final int httpStatus;
    private final String message;
}
