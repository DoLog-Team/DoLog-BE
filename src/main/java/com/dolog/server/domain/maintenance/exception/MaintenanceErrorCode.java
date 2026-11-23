package com.dolog.server.domain.maintenance.exception;

import com.dolog.server.global.response.code.BaseResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.dolog.server.global.constant.StaticValue.*;

@Getter
@AllArgsConstructor
public enum MaintenanceErrorCode implements BaseResponseCode {

    PERIOD_REQUIRED("MAINTENANCE_400_1", BAD_REQUEST, "점검을 시작하려면 시작/종료 시간을 모두 입력해야 합니다."),
    INVALID_PERIOD("MAINTENANCE_400_2", BAD_REQUEST, "점검 종료 시간은 시작 시간보다 늦어야 합니다.");

    private final String code;
    private final int httpStatus;
    private final String message;
}
