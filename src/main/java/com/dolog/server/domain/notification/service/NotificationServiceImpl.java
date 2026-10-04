package com.dolog.server.domain.notification.service;

import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.domain.notification.entity.Notification;
import com.dolog.server.domain.notification.entity.enums.NotificationType;
import com.dolog.server.domain.notification.exception.NotificationErrorCode;
import com.dolog.server.domain.notification.exception.NotificationException;
import com.dolog.server.domain.notification.repository.NotificationRepository;
import com.dolog.server.domain.notification.web.dto.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ExhibitionArtistMapRepository exhibitionArtistMapRepository;

    @Override
    public List<NotificationResponse> getMyNotifications(UUID accountId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(accountId).stream()
                .map(NotificationResponse::from)
                .toList();
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

    @Override
    @Transactional
    public void notifyJoinedArtists(UUID exhibitionId, NotificationType type, Map<String, String> payload, UUID referenceId) {
        exhibitionArtistMapRepository.findByExhibitionId(exhibitionId).stream()
                .filter(map -> map.getStatus() == ExhibitionArtistStatus.JOINED)
                .map(map -> map.getArtist().getAccount())
                .filter(Objects::nonNull)
                .forEach(account -> notificationRepository.save(Notification.builder()
                        .recipient(account)
                        .type(type)
                        .payload(payload)
                        .referenceId(referenceId)
                        .build()));
    }
}
