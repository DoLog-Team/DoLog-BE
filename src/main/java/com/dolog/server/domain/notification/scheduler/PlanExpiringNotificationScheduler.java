package com.dolog.server.domain.notification.scheduler;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.repository.ExhibitionDetailRepository;
import com.dolog.server.domain.notification.entity.enums.NotificationType;
import com.dolog.server.domain.notification.service.NotificationService;
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
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlanExpiringNotificationScheduler {

    private static final int DAYS_LEFT = 7;

    private final SubscriptionRepository subscriptionRepository;
    private final ExhibitionDetailRepository exhibitionDetailRepository;
    private final NotificationService notificationService;

    // 매일 오전 9시(한국 시간)에 종료일이 7일 남은 활성 구독의 전시 어드민에게 알림
    // 대상 구간을 하루 폭(6~7일 뒤)으로 잡아서, 같은 구독에 같은 알림이 반복되지 않게 한다
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    @Transactional
    public void notifyPlanExpiringSoon() {
        LocalDateTime now = LocalDateTime.now();
        List<Subscription> targets = subscriptionRepository.findByStatusAndEndedAtBetween(
                SubscriptionStatus.ACTIVE, now.plusDays(DAYS_LEFT - 1), now.plusDays(DAYS_LEFT)
        );

        for (Subscription subscription : targets) {
            Exhibition exhibition = subscription.getExhibition();
            notificationService.send(
                    exhibition.getAccount(),
                    NotificationType.PLAN_EXPIRING_SOON,
                    Map.of(
                            "exhibitionName", resolveExhibitionName(exhibition),
                            "daysLeft", String.valueOf(DAYS_LEFT)
                    ),
                    exhibition.getId()
            );
        }

        log.info("플랜 만료 임박 알림 {}건 발송", targets.size());
    }

    // 전시 이름은 상세 정보의 제목을 쓰고, 없으면 slug로 대신한다
    private String resolveExhibitionName(Exhibition exhibition) {
        return exhibitionDetailRepository.findByExhibitionId(exhibition.getId())
                .map(detail -> detail.getTitle())
                .orElse(exhibition.getSlug());
    }
}
