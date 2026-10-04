package com.dolog.server.domain.notification.service;

import com.dolog.server.domain.notification.entity.Notification;
import com.dolog.server.domain.notification.exception.NotificationErrorCode;
import com.dolog.server.domain.notification.exception.NotificationException;
import com.dolog.server.domain.notification.repository.NotificationRepository;
import com.dolog.server.domain.notification.web.dto.response.NotificationListResponse;
import com.dolog.server.domain.notification.web.dto.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository notificationRepository;

    @Override
    public NotificationListResponse getMyNotifications(UUID accountId, int size) {
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_SIZE_INVALID);
        }

        List<NotificationResponse> notifications = notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(accountId, PageRequest.of(0, size)).stream()
                .map(NotificationResponse::from)
                .toList();

        return NotificationListResponse.builder()
                .notifications(notifications)
                .unreadCount(notificationRepository.countByRecipientIdAndReadFalse(accountId))
                .build();
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID accountId) {
        // 내 알림이 아니면 찾을 수 없음으로 처리
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, accountId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        notification.markAsRead();

        return NotificationResponse.from(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID accountId) {
        notificationRepository.markAllAsRead(accountId);
    }

    @Override
    @Transactional
    public long deleteNotificationsCreatedBefore(LocalDateTime threshold) {
        return notificationRepository.deleteByCreatedAtBefore(threshold);
    }
}
