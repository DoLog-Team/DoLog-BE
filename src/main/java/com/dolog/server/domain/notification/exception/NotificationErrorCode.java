package com.dolog.server.domain.notification.exception;

import com.dolog.server.global.response.code.BaseResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.dolog.server.global.constant.StaticValue.*;

@Getter
@AllArgsConstructor
public enum NotificationErrorCode implements BaseResponseCode {

    NOTIFICATION_NOT_FOUND("NOTIFICATION_404_1", NOT_FOUND, "알림을 찾을 수 없습니다.");

    private final String code;
    private final int httpStatus;
    private final String message;
}
