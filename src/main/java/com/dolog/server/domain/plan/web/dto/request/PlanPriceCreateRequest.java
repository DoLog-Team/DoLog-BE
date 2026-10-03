package com.dolog.server.domain.plan.web.dto.request;

import com.dolog.server.domain.plan.entity.enums.BillingCycle;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class PlanPriceCreateRequest {

    @NotNull
    private BillingCycle billingCycle;

    @NotNull
    @Positive(message = "가격은 0보다 커야 합니다.")
    private BigDecimal price;

    @DecimalMin(value = "0", message = "할인율은 0 이상이어야 합니다.")
    @DecimalMax(value = "100", message = "할인율은 100 이하여야 합니다.")
    private BigDecimal discountRate;

    @NotNull
    @Positive(message = "개월 수는 0보다 커야 합니다.")
    private Integer months;
}
