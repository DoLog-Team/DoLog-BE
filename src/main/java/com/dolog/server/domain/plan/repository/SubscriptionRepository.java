package com.dolog.server.domain.plan.repository;

import com.dolog.server.domain.plan.entity.Subscription;
import com.dolog.server.domain.plan.entity.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    boolean existsByExhibitionIdAndStatus(UUID exhibitionId, SubscriptionStatus status);

    boolean existsByExhibitionIdAndStatusIn(UUID exhibitionId, Collection<SubscriptionStatus> statuses);

    Optional<Subscription> findFirstByExhibitionIdAndStatusInOrderByCreatedAtDesc(
            UUID exhibitionId, Collection<SubscriptionStatus> statuses);
}
