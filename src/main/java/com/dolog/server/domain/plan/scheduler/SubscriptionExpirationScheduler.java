package com.dolog.server.domain.plan.scheduler;

import com.dolog.server.domain.plan.entity.Subscription;
import com.dolog.server.domain.plan.entity.enums.SubscriptionStatus;
import com.dolog.server.domain.plan.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionExpirationScheduler {

    private final SubscriptionRepository subscriptionRepository;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void expireOverdueSubscriptions() {
        List<Subscription> overdue = subscriptionRepository
                .findByStatusAndEndedAtBefore(SubscriptionStatus.ACTIVE, LocalDateTime.now());

        overdue.forEach(Subscription::markAsExpired);

        log.info("만료 처리된 구독 {}건", overdue.size());
    }
}
