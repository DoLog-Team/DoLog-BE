package com.dolog.server.domain.artist.exception.artistError;

import com.dolog.server.global.response.code.BaseResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ArtistErrorCode implements BaseResponseCode {

    ARTIST_404_NOT_FOUND("ARTIST_404", 404, "작가를 찾을 수 없습니다."),
    ARTIST_400_INVALID_REQUEST("ARTIST_400", 400, "잘못된 작가 요청입니다."),
    ARTIST_409_ALREADY_EXISTS("ARTIST_409_ALREADY_EXISTS",409,"이미 작가 정보가 등록된 계정입니다."),
    ARTIST_409_DUPLICATE_PHONE("ARTIST_409_DUPLICATE_PHONE",409,"이미 사용 중인 전화번호입니다."),
    ARTIST_409_LINKED_DATA("ARTIST_409_LINKED_DATA", 409, "연결된 데이터가 있어 작가를 삭제할 수 없습니다.");

    private final String code;
    private final int httpStatus;
    private final String message;
}
