package com.dolog.server.domain.plan.web.dto.response;

import com.dolog.server.domain.plan.entity.Subscription;
import com.dolog.server.domain.plan.entity.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class SubscriptionCancelResponse {

    private UUID subscriptionId;
    private UUID exhibitionId;
    private SubscriptionStatus status;
    private LocalDateTime endedAt;
    private BigDecimal refundAmount;

    public static SubscriptionCancelResponse from(Subscription subscription, BigDecimal refundAmount) {
        return SubscriptionCancelResponse.builder()
                .subscriptionId(subscription.getId())
                .exhibitionId(subscription.getExhibition().getId())
                .status(subscription.getStatus())
                .endedAt(subscription.getEndedAt())
                .refundAmount(refundAmount)
                .build();
    }
}
