package com.dolog.server.domain.artwork.exception;

import com.dolog.server.global.response.code.BaseResponseCode; // 임포트 확인
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ArtworkErrorCode implements BaseResponseCode { // 인터페이스 구현 추가
    ARTWORK_NOT_FOUND(404, "ARTWORK_001", "작품 정보를 찾을 수 없습니다."),
    ARTWORK_IMAGE_NOT_FOUND(404, "ARTWORK_002", "상세 이미지 정보를 찾을 수 없습니다."),
    INVALID_ARTWORK_IMAGE(400, "ARTWORK_003", "해당 작품에 속하지 않는 이미지입니다."),
    FILE_UPLOAD_ERROR(500, "ARTWORK_004", "파일 업로드 중 오류가 발생했습니다."),
    INVALID_ORDER_REQUEST(400, "ARTWORK_005", "잘못된 정렬 요청입니다. prev/next는 둘 다 필요합니다."),
    ARTWORK_ARTIST_MAPPING_NOT_FOUND(404, "ARTWORK_006", "작품-작가 매핑 정보를 찾을 수 없습니다.");

    private final int httpStatus;
    private final String code;
    private final String message;
}