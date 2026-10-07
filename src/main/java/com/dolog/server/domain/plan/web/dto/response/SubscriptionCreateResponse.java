package com.dolog.server.domain.plan.web.dto.response;

import com.dolog.server.domain.plan.entity.Subscription;
import com.dolog.server.domain.plan.entity.enums.BillingCycle;
import com.dolog.server.domain.plan.entity.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class SubscriptionCreateResponse {

    private UUID subscriptionId;
    private UUID exhibitionId;
    private UUID planId;
    private String planName;
    private BillingCycle billingCycle;
    private BigDecimal paidAmount;
    private Integer months;
    private SubscriptionStatus status;
    private LocalDateTime createdAt;

    public static SubscriptionCreateResponse of(Subscription subscription) {
        return SubscriptionCreateResponse.builder()
                .subscriptionId(subscription.getId())
                .exhibitionId(subscription.getExhibition().getId())
                .planId(subscription.getPlan().getId())
                .planName(subscription.getPlan().getName())
                .billingCycle(subscription.getBillingCycle())
                .paidAmount(subscription.getPaidAmount())
                .months(subscription.getMonths())
                .status(subscription.getStatus())
                .createdAt(subscription.getCreatedAt())
                .build();
    }
}
