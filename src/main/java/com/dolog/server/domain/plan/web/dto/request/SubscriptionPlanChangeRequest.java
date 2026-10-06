package com.dolog.server.domain.plan.web.dto.request;

import com.dolog.server.domain.plan.entity.enums.BillingCycle;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class SubscriptionPlanChangeRequest {

    @NotNull
    private UUID targetPlanId;

    @NotNull
    private BillingCycle billingCycle;
}
