package com.dolog.server.domain.plan.repository;

import com.dolog.server.domain.plan.entity.Subscription;
import com.dolog.server.domain.plan.entity.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    boolean existsByAccountIdAndStatus(UUID accountId, SubscriptionStatus status);
}
