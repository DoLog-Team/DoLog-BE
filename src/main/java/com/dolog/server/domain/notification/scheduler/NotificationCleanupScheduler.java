package com.dolog.server.domain.notification.scheduler;

import com.dolog.server.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupScheduler {

    private static final int RETENTION_DAYS = 30;

    private final NotificationService notificationService;

    // 매일 새벽 4시(한국 시간)에 생성된 지 30일이 지난 알림을 삭제한다
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    public void deleteExpiredNotifications() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(RETENTION_DAYS);
        long deleted = notificationService.deleteNotificationsCreatedBefore(threshold);
        log.info("30일 지난 알림 {}건 삭제", deleted);
    }
}
