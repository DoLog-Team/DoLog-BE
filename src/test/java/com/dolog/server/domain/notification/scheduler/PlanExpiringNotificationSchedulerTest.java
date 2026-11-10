package com.dolog.server.domain.notification.scheduler;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.repository.ExhibitionDetailRepository;
import com.dolog.server.domain.notification.entity.enums.NotificationType;
import com.dolog.server.domain.notification.service.NotificationService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanExpiringNotificationSchedulerTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private ExhibitionDetailRepository exhibitionDetailRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PlanExpiringNotificationScheduler scheduler;

    @Test
    @DisplayName("종료일 D-7/D-3/D-1인 구독마다 각각 전시 어드민에게 알림이 간다")
    void notifiesAtEachCheckpoint() {
        Subscription d7 = subscription(7);
        Subscription d3 = subscription(3);
        Subscription d1 = subscription(1);

        LocalDateTime now = LocalDateTime.now();
        when(subscriptionRepository.findByStatusAndEndedAtBetween(
                eq(SubscriptionStatus.ACTIVE), any(), any()))
                .thenAnswer(invocation -> {
                    LocalDateTime from = invocation.getArgument(1);
                    long daysFromNow = java.time.Duration.between(now, from).toDays();
                    if (daysFromNow == 6) return List.of(d7);
                    if (daysFromNow == 2) return List.of(d3);
                    if (daysFromNow == 0) return List.of(d1);
                    return List.of();
                });
        when(exhibitionDetailRepository.findByExhibitionId(any())).thenReturn(java.util.Optional.empty());

        scheduler.notifyPlanExpiringSoon();

        verify(notificationService).send(eq(d7.getExhibition().getAccount()), eq(NotificationType.PLAN_EXPIRING_SOON),
                argThat(payload -> "7".equals(payload.get("daysLeft"))), eq(d7.getExhibition().getId()));
        verify(notificationService).send(eq(d3.getExhibition().getAccount()), eq(NotificationType.PLAN_EXPIRING_SOON),
                argThat(payload -> "3".equals(payload.get("daysLeft"))), eq(d3.getExhibition().getId()));
        verify(notificationService).send(eq(d1.getExhibition().getAccount()), eq(NotificationType.PLAN_EXPIRING_SOON),
                argThat(payload -> "1".equals(payload.get("daysLeft"))), eq(d1.getExhibition().getId()));
    }

    private Subscription subscription(int daysLeft) {
        Exhibition exhibition = Exhibition.builder().id(UUID.randomUUID()).slug("exp-" + daysLeft)
                .account(Account.builder().id(UUID.randomUUID()).build())
                .build();
        return Subscription.builder()
                .id(UUID.randomUUID())
                .exhibition(exhibition)
                .status(SubscriptionStatus.ACTIVE)
                .billingCycle(BillingCycle.MONTHLY)
                .months(1)
                .startedAt(LocalDateTime.now().minusMonths(1))
                .endedAt(LocalDateTime.now().plusDays(daysLeft))
                .build();
    }
}
