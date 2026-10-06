package com.dolog.server.domain.plan.exception;

import com.dolog.server.global.exception.BaseException;

public class SubscriptionException extends BaseException {
    public SubscriptionException(SubscriptionErrorCode errorCode) {
        super(errorCode);
    }
}
