package com.dolog.server.domain.plan.web.dto.response;

import com.dolog.server.domain.plan.entity.SubscriptionRefund;
import com.dolog.server.domain.plan.entity.enums.RefundStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class SubscriptionRefundResponse {

    private UUID refundId;
    private UUID subscriptionId;
    private UUID exhibitionId;
    private BigDecimal paidAmount;
    private Integer totalDays;
    private Integer usedDays;
    private BigDecimal refundAmount;
    private RefundStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;

    public static SubscriptionRefundResponse from(SubscriptionRefund refund) {
        return SubscriptionRefundResponse.builder()
                .refundId(refund.getId())
                .subscriptionId(refund.getSubscription().getId())
                .exhibitionId(refund.getExhibition().getId())
                .paidAmount(refund.getPaidAmount())
                .totalDays(refund.getTotalDays())
                .usedDays(refund.getUsedDays())
                .refundAmount(refund.getRefundAmount())
                .status(refund.getStatus())
                .requestedAt(refund.getRequestedAt())
                .completedAt(refund.getCompletedAt())
                .build();
    }
}
