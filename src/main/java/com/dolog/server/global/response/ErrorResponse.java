package com.dolog.server.global.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.dolog.server.global.response.code.BaseResponseCode;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@JsonPropertyOrder({"isSuccess", "timestamp", "code", "httpStatus", "message", "data"})
public class ErrorResponse<T> extends BaseResponse {
    private final int httpStatus;
    private final T data;

    @Builder
    public ErrorResponse(T data, String code, String message, int httpStatus) {
        super(false, code, message);
        this.data = data;
        this.httpStatus = httpStatus;
    }

    // 기본 메시지를 그대로 사용할 떄
    public static ErrorResponse<?> of(BaseResponseCode baseCode) {
        return ErrorResponse.builder()
                .code(baseCode.getCode())
                .message(baseCode.getMessage())
                .httpStatus(baseCode.getHttpStatus())
                .data(null)
                .build();
    }

    public static <T> ErrorResponse<T> of(BaseResponseCode baseCode, String message) {
        return ErrorResponse.<T>builder()
                .code(baseCode.getCode())
                .httpStatus(baseCode.getHttpStatus())
                .message(message)
                .data(null)
                .build();
    }

    public static <T> ErrorResponse<T> of(BaseResponseCode baseCode, String message, T data) {
        return ErrorResponse.<T>builder()
                .code(baseCode.getCode())
                .httpStatus(baseCode.getHttpStatus())
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ErrorResponse<T> of(String code, String message, int httpStatus) {
        return ErrorResponse.<T>builder()
                .code(code)
                .httpStatus(httpStatus)
                .message(message)
                .data(null)
                .build();
    }


}
