package com.dolog.server.domain.maintenance.exception;

import com.dolog.server.global.exception.BaseException;

public class MaintenanceException extends BaseException {
    public MaintenanceException(MaintenanceErrorCode errorCode) {
        super(errorCode);
    }
}
