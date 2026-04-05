package com.dolog.server.domain.account.exception;

import com.dolog.server.global.constant.StaticValue;
import com.dolog.server.global.response.code.BaseResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AccountErrorCode implements BaseResponseCode {

    DUPLICATE_EMAIL("ACCOUNT_409_1", StaticValue.CONFLICT, "이미 존재하는 이메일입니다."),
    INVALID_LOGIN("ACCOUNT_401_INVALID_LOGIN", StaticValue.UNAUTHORIZED, "이메일 또는 비밀번호가 잘못되었습니다."),
    INVALID_PASSWORD("ACCOUNT_401_PASSWORD", StaticValue.UNAUTHORIZED, "현재 비밀번호가 일치하지 않습니다.");

    private final String code;
    private final int httpStatus;
    private final String message;
}



