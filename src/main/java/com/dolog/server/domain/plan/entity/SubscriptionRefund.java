package com.dolog.server.domain.plan.entity;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.plan.entity.enums.RefundStatus;
import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "subscription_refunds")
public class SubscriptionRefund extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exhibition_id", nullable = false)
    private Exhibition exhibition;

    @Column(name = "paid_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "total_days", nullable = false)
    private Integer totalDays;

    @Column(name = "used_days", nullable = false)
    private Integer usedDays;

    @Column(name = "refund_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * 일 단위 일할 환불. 구독 시작일과 계획된 종료일을 날짜로 세고(양끝 포함),
     * 사용 일수는 시작일부터 해지한 날(usedUntil)까지 센다. 해지 후 유예기간은 사용 일수에 넣지 않는다.
     * 환불액 = 결제금액 × 남은 일수 / 전체 일수 (원 단위 버림)
     */
    public static SubscriptionRefund calculate(Subscription subscription, LocalDateTime requestedAt, LocalDateTime usedUntil) {
        LocalDate startDate = subscription.getStartedAt().toLocalDate();
        LocalDate plannedEndDate = subscription.getEndedAt().toLocalDate();

        int totalDays = (int) ChronoUnit.DAYS.between(startDate, plannedEndDate) + 1;
        int usedDays = Math.min((int) ChronoUnit.DAYS.between(startDate, usedUntil.toLocalDate()) + 1, totalDays);
        int remainingDays = totalDays - usedDays;

        BigDecimal paidAmount = Objects.requireNonNullElse(subscription.getPaidAmount(), BigDecimal.ZERO);
        BigDecimal refundAmount = paidAmount
                .multiply(BigDecimal.valueOf(remainingDays))
                .divide(BigDecimal.valueOf(totalDays), 0, RoundingMode.DOWN);

        return SubscriptionRefund.builder()
                .subscription(subscription)
                .exhibition(subscription.getExhibition())
                .paidAmount(paidAmount)
                .totalDays(totalDays)
                .usedDays(usedDays)
                .refundAmount(refundAmount)
                .status(RefundStatus.PENDING)
                .requestedAt(requestedAt)
                .build();
    }

    public void complete(LocalDateTime completedAt) {
        this.status = RefundStatus.COMPLETED;
        this.completedAt = completedAt;
    }
}
