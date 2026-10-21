package com.dolog.server.domain.plan.scheduler;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.plan.entity.Subscription;
import com.dolog.server.domain.plan.entity.enums.BillingCycle;
import com.dolog.server.domain.plan.entity.enums.SubscriptionStatus;
import com.dolog.server.domain.plan.repository.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionExpirationSchedulerTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private SubscriptionExpirationScheduler scheduler;

    @Test
    @DisplayName("만료된 구독을 EXPIRED로 바꾸고, 연결된 전시도 비공개로 전환한다")
    void expiresSubscriptionAndUnpublishesExhibition() {
        Exhibition exhibition = Exhibition.builder().isPublic(true).build();
        Subscription subscription = Subscription.builder()
                .exhibition(exhibition).status(SubscriptionStatus.ACTIVE)
                .billingCycle(BillingCycle.MONTHLY).months(1)
                .startedAt(LocalDateTime.now().minusMonths(2))
                .endedAt(LocalDateTime.now().minusDays(1))
                .build();

        when(subscriptionRepository.findByStatusAndEndedAtBefore(eq(SubscriptionStatus.ACTIVE), any()))
                .thenReturn(List.of(subscription));

        scheduler.expireOverdueSubscriptions();

        assertEquals(SubscriptionStatus.EXPIRED, subscription.getStatus());
        assertFalse(exhibition.isPublic());
    }
}
