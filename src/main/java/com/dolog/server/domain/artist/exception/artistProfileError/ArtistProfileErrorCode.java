package com.dolog.server.domain.artist.exception.artistProfileError;

import com.dolog.server.global.response.code.BaseResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ArtistProfileErrorCode implements BaseResponseCode {

    ARTIST_PROFILE_404_NOT_FOUND("ARTIST_PROFILE_404", 404, "작가 프로필을 찾을 수 없습니다."),
    ARTIST_PROFILE_400_INVALID_REQUEST("ARTIST_PROFILE_400", 400, "잘못된 작가 프로필 요청입니다."),
    ARTIST_PROFILE_409_ALREADY_EXISTS("ARTIST_PROFILE_409", 409, "이미 해당 전시에 등록된 작가 프로필이 존재합니다.");
    private final String code;
    private final int httpStatus;
    private final String message;
}