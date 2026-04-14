package com.dolog.server.global.response;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.dolog.server.global.response.code.BaseResponseCode;
import com.dolog.server.global.response.code.GlobalSuccessCode;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@JsonPropertyOrder({"isSuccess", "timestamp", "code", "httpStatus", "message", "data"})
public class SuccessResponse<T> extends BaseResponse{
    private final int httpStatus;
    private final T data;

    @Builder
    public SuccessResponse(T data, BaseResponseCode baseResponseCode) {
        super(true, baseResponseCode.getCode(), baseResponseCode.getMessage());
        this.data = data;
        this.httpStatus = baseResponseCode.getHttpStatus();
    }

    // 200 OK 응답
    public static <T> SuccessResponse<T> ok(T data) {
        return new SuccessResponse<>(data, GlobalSuccessCode.SUCCESS_OK);
    }

    public static <T> SuccessResponse<T> ok(T data, String message) {
        return new SuccessResponse<>(
                data,
                new BaseResponseCode() {
                    public String getCode() { return "SUCCESS_200"; }
                    public int getHttpStatus() { return 200; }
                    public String getMessage() { return message; }
                }
        );
    }

    // 201 Created 응답
    public static <T> SuccessResponse<T> created(T data) {
        return new SuccessResponse<>(data, GlobalSuccessCode.SUCCESS_CREATED);
    }

    public static <T> SuccessResponse<T> created(T data, String message) {
        return new SuccessResponse<>(
                data,
                new BaseResponseCode() {
                    public String getCode() { return "SUCCESS_201"; }
                    public int getHttpStatus() { return 201; }
                    public String getMessage() { return message; }
                }
        );
    }

    public static <T> SuccessResponse<T> empty() {
        return new SuccessResponse<>(null, GlobalSuccessCode.SUCCESS_OK);
    }

    public static <T> SuccessResponse<T> of(T data, BaseResponseCode baseResponseCode) {
        return new SuccessResponse<>(data, baseResponseCode);
    }

}
