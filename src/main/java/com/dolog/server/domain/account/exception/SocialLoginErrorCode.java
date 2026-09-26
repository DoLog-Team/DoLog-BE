package com.dolog.server.domain.account.exception;

import com.dolog.server.global.response.code.BaseResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SocialLoginErrorCode implements BaseResponseCode {
    INVALID_REDIRECT_URI("SOCIAL_400_REDIRECT_URI", 400, "허용되지 않은 소셜 로그인 콜백 주소입니다."),
    PROVIDER_NOT_SUPPORTED("SOCIAL_400_PROVIDER", 400, "아직 지원하지 않는 소셜 로그인 제공자입니다."),
    INVALID_AUTHORIZATION_CODE("SOCIAL_400_CODE", 400, "인가 코드가 유효하지 않습니다. 다시 로그인해 주세요."),
    TARGET_ROLE_NOT_ALLOWED("SOCIAL_403_ROLE", 403, "소셜 로그인 대상 계정이 아닙니다."),
    PROVIDER_ERROR("SOCIAL_502_PROVIDER", 502, "소셜 로그인 제공자 응답을 처리할 수 없습니다."),
    PROVIDER_UNAVAILABLE("SOCIAL_503_PROVIDER", 503, "소셜 로그인 제공자를 일시적으로 이용할 수 없습니다."),
    PROVIDER_TIMEOUT("SOCIAL_504_PROVIDER", 504, "소셜 로그인 제공자 응답 시간이 초과되었습니다."),
    SERVER_CONFIGURATION("SOCIAL_500_CONFIG", 500, "소셜 로그인 서버 설정을 확인해 주세요.");

    private final String code;
    private final int httpStatus;
    private final String message;
}
