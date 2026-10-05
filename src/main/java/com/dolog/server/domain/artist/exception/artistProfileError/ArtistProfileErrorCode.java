package com.dolog.server.domain.artist.exception.artistProfileError;

import com.dolog.server.global.response.code.BaseResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ArtistProfileErrorCode implements BaseResponseCode {

    ARTIST_PROFILE_404_NOT_FOUND("ARTIST_PROFILE_404", 404, "작가 프로필을 찾을 수 없습니다."),
    ARTIST_PROFILE_400_INVALID_REQUEST("ARTIST_PROFILE_400", 400, "잘못된 작가 프로필 요청입니다."),
    ARTIST_PROFILE_400_INVALID_IMAGE("ARTIST_PROFILE_IMAGE_400", 400, "프로필 이미지는 JPG, JPEG, PNG 형식의 5MB 이하 파일이어야 합니다."),
    ARTIST_PROFILE_403_ACCESS_DENIED("ARTIST_PROFILE_403", 403, "해당 작가 프로필을 수정할 권한이 없습니다."),
    ARTIST_PROFILE_409_ALREADY_EXISTS("ARTIST_PROFILE_409", 409, "이미 해당 전시에 등록된 작가 프로필이 존재합니다."),

    ARTIST_PROFILE_400_NOT_REGISTERED_IN_EXHIBITION("ARTIST_PROFILE_400_1", 400, "해당 전시에 등록된 작가가 아닙니다. 먼저 작가를 전시에 추가해주세요."),
    ARTIST_SNS_404_NOT_FOUND("ARTIST_SNS_404", 404, "해당 SNS 기록을 찾을 수 없습니다.");
    private final String code;
    private final int httpStatus;
    private final String message;
}
