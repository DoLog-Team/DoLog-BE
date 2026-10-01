package com.dolog.server.domain.plan.entity;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.plan.entity.enums.BillingCycle;
import com.dolog.server.domain.plan.entity.enums.SubscriptionStatus;
import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "subscriptions")
public class Subscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exhibition_id", nullable = false)
    private Exhibition exhibition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private Plan plan;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false)
    private BillingCycle billingCycle;

    @Column(nullable = false)
    private Integer months;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    public void updateStatus(SubscriptionStatus status) {
        this.status = status;
        if (status == SubscriptionStatus.ACTIVE && this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
            this.endedAt = this.startedAt.plusMonths(this.months);
        }
        if (status == SubscriptionStatus.CANCELED || status == SubscriptionStatus.EXPIRED) {
            // 자연 만료 전 조기 해지/종료된 경우, 계획된 만료일(endedAt)을 실제 종료 시각으로 덮어씀
            this.endedAt = LocalDateTime.now();
        }
    }

    public void changePlan(Plan plan, BillingCycle billingCycle, Integer months) {
        this.plan = plan;
        this.billingCycle = billingCycle;
        this.months = months;
    }

    public void cancelSubscription() {
        this.status = SubscriptionStatus.CANCELED;
        this.endedAt = LocalDateTime.now();
    }
}
