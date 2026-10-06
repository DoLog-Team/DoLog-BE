package com.dolog.server.domain.plan.web.dto.request;

import com.dolog.server.domain.plan.entity.enums.BillingCycle;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class SubscriptionCreateRequest {

    @NotNull
    private UUID planId;

    @NotNull
    private BillingCycle billingCycle;
}
