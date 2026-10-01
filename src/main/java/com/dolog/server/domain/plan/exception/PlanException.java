package com.dolog.server.domain.plan.exception;

import com.dolog.server.global.exception.BaseException;

public class PlanException extends BaseException {
    public PlanException(PlanErrorCode errorCode) {
        super(errorCode);
    }
}
