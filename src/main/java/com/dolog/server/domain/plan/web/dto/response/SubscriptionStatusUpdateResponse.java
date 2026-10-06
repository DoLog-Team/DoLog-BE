package com.dolog.server.domain.plan.web.dto.response;

import com.dolog.server.domain.plan.entity.Subscription;
import com.dolog.server.domain.plan.entity.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class SubscriptionStatusUpdateResponse {

    private UUID subscriptionId;
    private UUID exhibitionId;
    private SubscriptionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    public static SubscriptionStatusUpdateResponse from(Subscription subscription) {
        return SubscriptionStatusUpdateResponse.builder()
                .subscriptionId(subscription.getId())
                .exhibitionId(subscription.getExhibition().getId())
                .status(subscription.getStatus())
                .startedAt(subscription.getStartedAt())
                .endedAt(subscription.getEndedAt())
                .build();
    }
}
