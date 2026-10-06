package com.dolog.server.domain.notification.exception;

import com.dolog.server.global.exception.BaseException;
import com.dolog.server.global.response.code.BaseResponseCode;

public class NotificationException extends BaseException {
    public NotificationException(BaseResponseCode errorCode) {
        super(errorCode);
    }
}
