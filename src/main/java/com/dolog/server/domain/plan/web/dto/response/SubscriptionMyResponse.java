package com.dolog.server.domain.plan.web.dto.response;

import com.dolog.server.domain.plan.entity.Subscription;
import com.dolog.server.domain.plan.entity.enums.BillingCycle;
import com.dolog.server.domain.plan.entity.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class SubscriptionMyResponse {

    private UUID subscriptionId;
    private UUID exhibitionId;
    private String exhibitionName;
    private UUID planId;
    private String planName;
    private BillingCycle billingCycle;
    private SubscriptionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    public static SubscriptionMyResponse of(Subscription subscription, String exhibitionName) {
        return SubscriptionMyResponse.builder()
                .subscriptionId(subscription.getId())
                .exhibitionId(subscription.getExhibition().getId())
                .exhibitionName(exhibitionName)
                .planId(subscription.getPlan().getId())
                .planName(subscription.getPlan().getName())
                .billingCycle(subscription.getBillingCycle())
                .status(subscription.getStatus())
                .startedAt(subscription.getStartedAt())
                .endedAt(subscription.getEndedAt())
                .build();
    }
}
