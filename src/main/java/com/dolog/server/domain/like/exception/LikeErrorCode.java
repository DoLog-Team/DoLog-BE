package com.dolog.server.domain.like.exception;

import com.dolog.server.global.response.code.BaseResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LikeErrorCode implements BaseResponseCode {
    ALREADY_LIKED(409, "LIKE_001", "이미 좋아요를 눌렀습니다."),
    LIKE_NOT_FOUND(404, "LIKE_002", "좋아요 기록이 없습니다."),
    INVALID_VISITOR_ID(400, "LIKE_003", "방문자 식별자 형식이 올바르지 않습니다.");

    private final int httpStatus;
    private final String code;
    private final String message;
}
