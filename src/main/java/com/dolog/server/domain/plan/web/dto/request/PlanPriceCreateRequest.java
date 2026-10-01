package com.dolog.server.domain.plan.web.dto.request;

import com.dolog.server.domain.plan.entity.enums.BillingCycle;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class PlanPriceCreateRequest {

    @NotNull
    private BillingCycle billingCycle;

    @NotNull
    private BigDecimal price;

    private BigDecimal discountRate;

    @NotNull
    private Integer months;
}
