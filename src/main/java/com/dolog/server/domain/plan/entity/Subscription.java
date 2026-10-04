package com.dolog.server.domain.plan.entity;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.plan.entity.enums.BillingCycle;
import com.dolog.server.domain.plan.entity.enums.SubscriptionStatus;
import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    // 신청 시점의 실제 결제금액 스냅샷 (price × (1 - discountRate/100))
    @Column(name = "paid_amount", precision = 10, scale = 2)
    private BigDecimal paidAmount;

    public void updateStatus(SubscriptionStatus status) {
        this.status = status;
        if (status == SubscriptionStatus.ACTIVE && this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
            this.endedAt = calculatePlannedEnd(this.startedAt, this.months);
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

    /**
     * 해지 시 종료 시각을 지정한다. ACTIVE 구독은 유예기간 끝(23:59:59)을, 미결제 구독은 해지 시각을 넘긴다.
     */
    public void cancelSubscription(LocalDateTime endedAt) {
        this.status = SubscriptionStatus.CANCELED;
        this.endedAt = endedAt;
    }

    /**
     * 구독 시작일부터 months개월 뒤 전날 23:59:59까지를 계획 종료 시각으로 본다.
     * 예: 10/4 시작, 1개월 → 11/3 23:59:59
     */
    public static LocalDateTime calculatePlannedEnd(LocalDateTime startedAt, Integer months) {
        return startedAt.toLocalDate().plusMonths(months).minusDays(1).atTime(LocalTime.of(23, 59, 59));
    }

    /**
     * 배치로 자연 만료 처리할 때 사용. endedAt은 이미 가입 시점에 계획된 값이라 건드리지 않음
     * (updateStatus()는 조기종료 시나리오를 가정해 endedAt을 현재 시각으로 덮어쓰므로 여기선 쓰지 않음).
     */
    public void markAsExpired() {
        this.status = SubscriptionStatus.EXPIRED;
    }
}
