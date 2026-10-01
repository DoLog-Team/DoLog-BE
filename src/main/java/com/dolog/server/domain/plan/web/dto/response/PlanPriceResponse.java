package com.dolog.server.domain.plan.web.dto.response;

import com.dolog.server.domain.plan.entity.PlanPrice;
import com.dolog.server.domain.plan.entity.enums.BillingCycle;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@Builder
public class PlanPriceResponse {

    private BillingCycle billingCycle;
    private BigDecimal price;
    private BigDecimal discountRate;
    private BigDecimal discountedPrice;
    private BigDecimal monthlyPrice;
    private Integer months;

    public static PlanPriceResponse from(PlanPrice planPrice) {
        BigDecimal discountedPrice = planPrice.resolveDiscountedPrice();
        BigDecimal monthlyPrice = discountedPrice.divide(
                BigDecimal.valueOf(planPrice.getMonths()), 0, RoundingMode.HALF_UP
        );

        return PlanPriceResponse.builder()
                .billingCycle(planPrice.getBillingCycle())
                .price(planPrice.getPrice())
                .discountRate(planPrice.getDiscountRate())
                .discountedPrice(discountedPrice)
                .monthlyPrice(monthlyPrice)
                .months(planPrice.getMonths())
                .build();
    }
}
