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
    ARTWORK_ARTIST_MAPPING_NOT_FOUND(404, "ARTWORK_006", "작품-작가 매핑 정보를 찾을 수 없습니다."),
    ARTWORK_ARTIST_MISMATCH(403, "ARTWORK_007", "본인 작가 정보로만 작품을 등록할 수 있습니다."),
    INVALID_IMAGE_FILE(400, "ARTWORK_008", "jpg, jpeg, png, webp 이미지만 올릴 수 있습니다."),
    ARTWORK_ALREADY_SUBMITTED(409, "ARTWORK_009", "이미 전시에 출품된 작품입니다."),
    ARTWORK_NOT_SUBMITTED(400, "ARTWORK_010", "전시에 출품되지 않은 작품입니다."),
    ARTIST_NOT_JOINED_EXHIBITION(400, "ARTWORK_011", "참여 중인 전시에만 출품할 수 있습니다."),
    INVALID_EXHIBITION_ZONE(400, "ARTWORK_012", "해당 전시에 속한 구역이 아닙니다."),
    REQUIRED_FIELDS_MISSING(400, "ARTWORK_013", "전시에서 정한 필수 항목을 채워야 공개할 수 있습니다."),
    NOT_EXHIBITION_OWNER(403, "ARTWORK_014", "해당 전시의 관리자만 처리할 수 있습니다."),
    REQUIRED_FIELD_HIDDEN(400, "ARTWORK_015", "필수 항목은 숨길 수 없습니다."),
    CO_ARTIST_NOT_IN_EXHIBITION(400, "ARTWORK_016", "같은 전시에 참여 중인 작가만 공동 작가로 등록할 수 있습니다."),
    CO_ARTIST_ALREADY_LINKED(409, "ARTWORK_017", "이미 이 작품에 연결된 작가입니다."),
    LAST_ARTIST_CANNOT_BE_REMOVED(400, "ARTWORK_018", "작품에는 작가가 한 명 이상 있어야 합니다."),
    IMAGE_FILE_REQUIRED(400, "ARTWORK_019", "등록할 이미지 파일을 하나 이상 보내야 합니다.");

    private final int httpStatus;
    private final String code;
    private final String message;
}