package com.dolog.server.domain.like.exception;

import com.dolog.server.global.exception.BaseException;

public class LikeException extends BaseException {

    public LikeException(LikeErrorCode errorCode) {
        super(errorCode);
    }
}
