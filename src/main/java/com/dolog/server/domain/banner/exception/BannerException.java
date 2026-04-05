package com.dolog.server.domain.banner.exception;

import com.dolog.server.global.exception.BaseException;

public class BannerException extends BaseException {
    public BannerException(BannerErrorCode errorCode) {
        super(errorCode);
    }
}
