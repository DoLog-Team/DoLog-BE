package com.dolog.server.domain.plan.entity;

import com.dolog.server.domain.plan.entity.enums.BillingCycle;
import com.dolog.server.domain.plan.entity.enums.TargetSize;
import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "plans")
public class Plan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(length = 100)
    private String name;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 300)
    private String description;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "max_artwork_count")
    private Integer maxArtworkCount;

    @Column(name = "min_commitment_months")
    private Integer minCommitmentMonths;

    @Builder.Default
    @Column(name = "is_popular")
    private Boolean isPopular = false;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Builder.Default
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanPrice> prices = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanTargetSize> targetSizes = new ArrayList<>();

    public void updateActiveStatus(Boolean isActive) {
        this.isActive = isActive;
    }

    public void updateBasicInfo(String name, String description, Integer maxArtworkCount,
                                 Integer minCommitmentMonths, Boolean isPopular, Integer displayOrder) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (maxArtworkCount != null) this.maxArtworkCount = maxArtworkCount;
        if (minCommitmentMonths != null) this.minCommitmentMonths = minCommitmentMonths;
        if (isPopular != null) this.isPopular = isPopular;
        if (displayOrder != null) this.displayOrder = displayOrder;
    }

    public void replaceTargetSizes(List<TargetSize> newTargetSizes) {
        this.targetSizes.clear();
        newTargetSizes.forEach(targetSize -> this.targetSizes.add(
                PlanTargetSize.builder().plan(this).targetSize(targetSize).build()
        ));
    }

    public void upsertPrice(BillingCycle billingCycle, BigDecimal price, BigDecimal discountRate, Integer months) {
        this.prices.stream()
                .filter(planPrice -> planPrice.getBillingCycle() == billingCycle)
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.update(price, discountRate, months),
                        () -> this.prices.add(
                                PlanPrice.builder()
                                        .plan(this)
                                        .billingCycle(billingCycle)
                                        .price(price)
                                        .discountRate(discountRate)
                                        .months(months)
                                        .build()
                        )
                );
    }

    public Optional<PlanPrice> findPriceByCycle(BillingCycle billingCycle) {
        return this.prices.stream()
                .filter(planPrice -> planPrice.getBillingCycle() == billingCycle)
                .findFirst();
    }
}
