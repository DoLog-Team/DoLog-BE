package com.dolog.server.domain.notification.service;

import com.dolog.server.domain.notification.web.dto.response.NotificationListResponse;
import com.dolog.server.domain.notification.web.dto.response.NotificationResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public interface NotificationService {

    // 내 알림 목록 (최신순, size개까지) + 읽지 않은 알림 수
    NotificationListResponse getMyNotifications(UUID accountId, int size);

    // 내 알림 한 건 읽음 처리
    NotificationResponse markAsRead(UUID notificationId, UUID accountId);

    // 내 읽지 않은 알림 전부 읽음 처리
    void markAllAsRead(UUID accountId);

    // 기준 시각 이전에 생성된 알림 삭제 (삭제 건수 반환)
    long deleteNotificationsCreatedBefore(LocalDateTime threshold);
}
