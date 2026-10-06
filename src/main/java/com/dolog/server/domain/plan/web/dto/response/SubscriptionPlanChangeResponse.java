package com.dolog.server.domain.plan.web.dto.response;

import com.dolog.server.domain.plan.entity.Subscription;
import com.dolog.server.domain.plan.entity.enums.BillingCycle;
import com.dolog.server.domain.plan.entity.enums.PlanChangeStatus;
import com.dolog.server.domain.plan.entity.enums.PlanChangeType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class SubscriptionPlanChangeResponse {

    private UUID subscriptionId;
    private UUID exhibitionId;
    private UUID previousPlanId;
    private String previousPlanName;
    private UUID targetPlanId;
    private String targetPlanName;
    private BillingCycle billingCycle;
    private PlanChangeType changeType;
    private PlanChangeStatus changeStatus;
    private LocalDateTime effectiveAt;

    public static SubscriptionPlanChangeResponse of(Subscription subscription, UUID previousPlanId,
                                                      String previousPlanName, PlanChangeType changeType) {
        return SubscriptionPlanChangeResponse.builder()
                .subscriptionId(subscription.getId())
                .exhibitionId(subscription.getExhibition().getId())
                .previousPlanId(previousPlanId)
                .previousPlanName(previousPlanName)
                .targetPlanId(subscription.getPlan().getId())
                .targetPlanName(subscription.getPlan().getName())
                .billingCycle(subscription.getBillingCycle())
                .changeType(changeType)
                .changeStatus(PlanChangeStatus.APPLIED)
                .effectiveAt(subscription.getUpdatedAt())
                .build();
    }
}
