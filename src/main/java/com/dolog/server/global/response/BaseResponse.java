package com.dolog.server.global.response;

import com.dolog.server.global.response.code.BaseResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@ToString
@RequiredArgsConstructor
public class BaseResponse {
    private final Boolean isSuccess;

    private final String code;

    private final String message;

    private final String timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    public static BaseResponse of(Boolean isSuccess, BaseResponseCode baseCode) {
        return new BaseResponse(isSuccess, baseCode.getCode(), baseCode.getMessage());
    }

    public static BaseResponse of(Boolean isSuccess, BaseResponseCode baseCode, String message) {
        return new BaseResponse(isSuccess, baseCode.getCode(), message);
    }

    public static BaseResponse of(Boolean isSuccess, String code, String message) {
        return new BaseResponse(isSuccess, code, message);
    }

}
