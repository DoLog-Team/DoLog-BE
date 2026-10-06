package com.dolog.server.domain.plan.exception;

import com.dolog.server.global.response.code.BaseResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.dolog.server.global.constant.StaticValue.*;

@Getter
@AllArgsConstructor
public enum PlanErrorCode implements BaseResponseCode {

    PLAN_NOT_FOUND("PLAN_404_1", NOT_FOUND, "요금제를 찾을 수 없습니다.");

    private final String code;
    private final int httpStatus;
    private final String message;
}
