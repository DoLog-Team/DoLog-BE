package com.dolog.server.domain.plan.repository;

import com.dolog.server.domain.plan.entity.SubscriptionRefund;
import com.dolog.server.domain.plan.entity.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubscriptionRefundRepository extends JpaRepository<SubscriptionRefund, UUID> {

    List<SubscriptionRefund> findByStatusOrderByRequestedAtDesc(RefundStatus status);

    List<SubscriptionRefund> findAllByOrderByRequestedAtDesc();
}
