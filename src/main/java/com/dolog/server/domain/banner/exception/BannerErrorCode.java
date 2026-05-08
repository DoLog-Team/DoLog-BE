package com.dolog.server.domain.banner.exception;

import com.dolog.server.global.response.code.BaseResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.dolog.server.global.constant.StaticValue.*;

@Getter
@AllArgsConstructor
public enum BannerErrorCode implements BaseResponseCode {

    BANNER_NOT_FOUND("BANNER_404_1", NOT_FOUND, "배너를 찾을 수 없습니다."),
    BANNER_IMAGE_UPLOAD_FAILED("BANNER_500_1", INTERNAL_SERVER_ERROR, "배너 이미지 업로드 중 오류가 발생했습니다.");

    private final String code;
    private final int httpStatus;
    private final String message;
}
