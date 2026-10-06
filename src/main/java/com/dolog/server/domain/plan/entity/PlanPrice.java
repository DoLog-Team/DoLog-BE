package com.dolog.server.domain.plan.entity;

import com.dolog.server.domain.plan.entity.enums.BillingCycle;
import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        name = "plan_prices",
        uniqueConstraints = @UniqueConstraint(name = "uk_plan_prices_cycle", columnNames = {"plan_id", "billing_cycle"})
)
public class PlanPrice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false)
    private BillingCycle billingCycle;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "discount_rate", precision = 5, scale = 2)
    private BigDecimal discountRate;

    @Column(nullable = false)
    private Integer months;

    public void update(BigDecimal price, BigDecimal discountRate, Integer months) {
        this.price = price;
        this.discountRate = discountRate;
        this.months = months;
    }

    public BigDecimal resolveDiscountedPrice() {
        if (discountRate == null) {
            return price;
        }
        BigDecimal rate = BigDecimal.ONE.subtract(
                discountRate.divide(BigDecimal.valueOf(100))
        );
        return price.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
