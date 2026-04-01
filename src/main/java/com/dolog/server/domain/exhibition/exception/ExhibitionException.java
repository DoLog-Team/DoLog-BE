package com.dolog.server.domain.exhibition.exception;

import com.dolog.server.global.exception.BaseException;
import com.dolog.server.global.response.code.BaseResponseCode;

public class ExhibitionException extends BaseException {
    public ExhibitionException(BaseResponseCode errorCode) {
        super(errorCode);
    }
}
