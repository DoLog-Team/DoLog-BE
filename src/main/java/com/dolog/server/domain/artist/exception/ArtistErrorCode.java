package com.dolog.server.domain.artist.exception;

import com.dolog.server.global.response.code.BaseResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ArtistErrorCode implements BaseResponseCode {

    ARTIST_404_NOT_FOUND("ARTIST_404", 404, "작가를 찾을 수 없습니다."),
    ARTIST_400_INVALID_REQUEST("ARTIST_400", 400, "잘못된 작가 요청입니다.");

    private final String code;
    private final int httpStatus;
    private final String message;
}
