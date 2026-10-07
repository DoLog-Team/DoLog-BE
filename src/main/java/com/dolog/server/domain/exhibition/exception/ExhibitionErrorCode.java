package com.dolog.server.domain.exhibition.exception;

import com.dolog.server.global.response.code.BaseResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.dolog.server.global.constant.StaticValue.*;

@Getter
@AllArgsConstructor
public enum ExhibitionErrorCode implements BaseResponseCode {

    ENTRY_CODE_INVALID("EXHIBITION_ENTRY_CODE_400", BAD_REQUEST, "유효하지 않은 전시 로그인 코드입니다."),
    ENTRY_CODE_EXPIRED("EXHIBITION_ENTRY_CODE_403", FORBIDDEN, "전시 로그인 코드가 만료되었습니다."),
    ARTIST_JOIN_CODE_INVALID("EXHIBITION_ARTIST_JOIN_CODE_400", BAD_REQUEST, "유효하지 않은 작가 참여 코드입니다."),
    ARTIST_JOIN_CODE_EXPIRED("EXHIBITION_ARTIST_JOIN_CODE_403", FORBIDDEN, "작가 참여 코드가 만료되었습니다."),
    ARTIST_JOIN_CODE_EXPIRY_INVALID("EXHIBITION_ARTIST_JOIN_CODE_EXPIRY_400", BAD_REQUEST, "코드 만료 시각은 현재보다 이후여야 합니다."),
    EXHIBITION_ARTIST_ALREADY_APPLIED("EXHIBITION_ARTIST_409", CONFLICT, "이미 참여 신청 중이거나 참여 중인 전시입니다."),
    EXHIBITION_ARTIST_STATUS_INVALID("EXHIBITION_ARTIST_STATUS_400", BAD_REQUEST, "변경할 수 없는 참여 상태입니다."),
    ENTRY_CODE_EXPIRY_INVALID("EXHIBITION_ENTRY_CODE_EXPIRY_400", BAD_REQUEST, "코드 만료 시각은 현재보다 이후여야 합니다."),
    EXHIBITION_EXPIRED("EXHIBITION_EXPIRED_403", FORBIDDEN, "사용 기간이 만료되었어요. 연장 희망 시, 두록에 문의해주세요."),

    EXHIBITION_NOT_FOUND("EXHIBITION_404_1", NOT_FOUND, "전시회를 찾을 수 없습니다."),
    EXHIBITION_UNAUTHORIZED("EXHIBITION_401_1", UNAUTHORIZED, "전시회 등록 권한이 없습니다. 로그인이 필요합니다."),

    EXHIBITION_MAP_ALREADY_EXISTS("EXHIBITION_MAP_400", BAD_REQUEST, "이미 장소 정보가 등록된 전시회입니다."),
    EXHIBITION_MAP_NOT_FOUND("EXHIBITION_MAP_404", NOT_FOUND, "등록된 장소 정보가 없습니다."),

    EXHIBITION_ARTIST_ALREADY_EXISTS("EXHIBITION_ARTIST_409", CONFLICT, "이미 전시에 추가된 작가입니다."),
    EXHIBITION_ARTIST_NOT_FOUND("EXHIBITION_ARTIST_404", NOT_FOUND, "해당 전시에 등록된 작가가 아닙니다."),
    EXHIBITION_ARTIST_NOT_JOINED("EXHIBITION_ARTIST_404_2", NOT_FOUND, "참여 중인 전시가 아닙니다."),
    EXHIBITION_ARTIST_QUERY_INVALID("EXHIBITION_ARTIST_QUERY_400", BAD_REQUEST, "잘못된 전시 작가 조회 조건입니다."),

    HOST_NOT_FOUND("EXHIBITION_HOST_404", NOT_FOUND, "등록된 호스트 정보가 없습니다."),

    ZONE_NOT_FOUND("EXHIBITION_ZONE_404", NOT_FOUND, "해당 ID의 구역을 찾을 수 없습니다."),

    EXHIBITION_NOT_PUBLIC("EXHIBITION_403_1", FORBIDDEN, "비공개 전시회입니다."),
    EXHIBITION_NOT_OWNER("EXHIBITION_403_2", FORBIDDEN, "본인 전시회만 관리할 수 있습니다."),
    SPLASH_PLAN_NOT_SUPPORTED("EXHIBITION_SPLASH_403", FORBIDDEN, "현재 플랜에서 스플래시 기능을 사용할 수 없습니다."),
    SPLASH_INVALID_IMAGE_URL("EXHIBITION_SPLASH_400", BAD_REQUEST, "잘못된 이미지 형식 또는 URL입니다."),
    EXHIBITION_DETAIL_NOT_FOUND("EXHIBITION_DETAIL_404", NOT_FOUND, "전시회 상세 정보를 찾을 수 없습니다."),

    PARTNER_NOT_FOUND("EXHIBITION_PARTNER_404", NOT_FOUND, "존재하지 않는 파트입니다."),

    PARTNER_MEMBER_NOT_FOUND("EXHIBITION_PARTNER_MEMBER_404", NOT_FOUND, "존재하지 않는 멤버입니다."),

    CUSTOM_THEME_NOT_FOUND("EXHIBITION_CUSTOM_THEME_404", NOT_FOUND, "커스텀 설정이 등록되지 않은 전시회입니다."),

    EXHIBITION_IMAGE_REQUIRED("EXHIBITION_400_1", BAD_REQUEST, "전시회 이미지는 필수 입력 항목입니다."),

    EXHIBITION_SLUG_NOT_FOUND("EXHIBITION_SLUG_404", NOT_FOUND, "해당 slug의 전시회를 찾을 수 없습니다."),
    EXHIBITION_SLUG_DUPLICATE("EXHIBITION_SLUG_409", CONFLICT, "이미 사용 중인 slug입니다."),
    EXHIBITION_SLUG_INVALID("EXHIBITION_SLUG_400", BAD_REQUEST, "slug는 공백일 수 없습니다."),

    EXHIBITION_ALREADY_PUBLISHED("EXHIBITION_409_1", CONFLICT, "이미 게시된 전시입니다."),
    EXHIBITION_NOT_PUBLISHED("EXHIBITION_409_2", CONFLICT, "게시되지 않은 전시입니다."),
    EXHIBITION_EXPIRES_AT_INVALID("EXHIBITION_400_2", BAD_REQUEST, "게시 종료 시각은 현재보다 이후여야 합니다.");

    private final String code;
    private final int httpStatus;
    private final String message;
}
