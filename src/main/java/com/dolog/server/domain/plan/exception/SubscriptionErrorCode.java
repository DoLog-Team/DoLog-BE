package com.dolog.server.domain.plan.exception;

import com.dolog.server.global.response.code.BaseResponseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.dolog.server.global.constant.StaticValue.*;

@Getter
@AllArgsConstructor
public enum SubscriptionErrorCode implements BaseResponseCode {

    SUBSCRIPTION_NOT_FOUND("SUBSCRIPTION_404_1", NOT_FOUND, "구독 정보를 찾을 수 없습니다."),
    PLAN_PRICE_NOT_FOUND("SUBSCRIPTION_400_1", BAD_REQUEST, "선택한 결제 주기의 가격 정보가 없습니다."),
    ALREADY_SUBSCRIBED("SUBSCRIPTION_400_2", BAD_REQUEST, "이미 진행 중인 구독이 있습니다. 변경은 구독 플랜 변경 API를 이용해주세요."),
    PLAN_NOT_ACTIVE("SUBSCRIPTION_400_3", BAD_REQUEST, "신규 신청이 불가능한 요금제입니다."),
    SUBSCRIPTION_REFUND_NOT_FOUND("SUBSCRIPTION_404_2", NOT_FOUND, "환불 정보를 찾을 수 없습니다."),
    SUBSCRIPTION_REFUND_ALREADY_COMPLETED("SUBSCRIPTION_409_1", CONFLICT, "이미 환불 완료 처리된 건입니다.");

    private final String code;
    private final int httpStatus;
    private final String message;
}
