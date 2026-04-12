package com.dolog.server.domain.bts.exception;

import com.dolog.server.global.exception.BaseException;
import com.dolog.server.global.response.code.BaseResponseCode;

public class BtsException extends BaseException {
    public BtsException(BaseResponseCode errorCode) {
        super(errorCode);
    }
}
