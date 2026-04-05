package com.dolog.server.domain.account.exception;

import com.dolog.server.global.exception.BaseException;

public class InvalidLoginException extends BaseException {

    public InvalidLoginException() {
        super(AccountErrorCode.INVALID_LOGIN);
    }
}