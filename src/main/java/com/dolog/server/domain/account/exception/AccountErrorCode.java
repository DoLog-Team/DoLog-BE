package com.dolog.server.domain.account.exception;

import com.dolog.server.global.constant.StaticValue;
import com.dolog.server.global.response.code.BaseResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AccountErrorCode implements BaseResponseCode {

    DUPLICATE_EMAIL("ACCOUNT_409_1", StaticValue.CONFLICT, "이미 존재하는 이메일입니다."),
    ACCOUNT_INACTIVE("ACCOUNT_403_INACTIVE", StaticValue.FORBIDDEN, "이용할 수 없는 계정입니다."),
    INVALID_LOGIN("ACCOUNT_401_INVALID_LOGIN", StaticValue.UNAUTHORIZED, "이메일 또는 비밀번호가 잘못되었습니다."),
    INVALID_PASSWORD("ACCOUNT_401_PASSWORD", StaticValue.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다."),
    TERMS_VERSION_MISMATCH("ACCOUNT_400_TERMS_VERSION", StaticValue.BAD_REQUEST, "약관 버전이 최신이 아닙니다. 약관을 다시 확인해 주세요."),
    TERMS_REQUIRED_MISSING("ACCOUNT_400_TERMS_REQUIRED", StaticValue.BAD_REQUEST, "필수 약관에 모두 동의해야 합니다."),
    TERMS_NOT_CONFIGURED("ACCOUNT_500_TERMS_CONFIG", StaticValue.INTERNAL_SERVER_ERROR, "약관 버전 설정을 확인해 주세요.");

    private final String code;
    private final int httpStatus;
    private final String message;
}


