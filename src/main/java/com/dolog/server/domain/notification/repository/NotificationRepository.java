package com.dolog.server.domain.notification.repository;

import com.dolog.server.domain.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // 내 알림 목록 (최신순, pageable 크기만큼)
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);

    // 내 읽지 않은 알림 수
    long countByRecipientIdAndReadFalse(UUID recipientId);

    // 내 알림 한 건 (다른 사람 알림이면 조회되지 않음)
    Optional<Notification> findByIdAndRecipientId(UUID id, UUID recipientId);

    // 내 읽지 않은 알림 전부 읽음 처리
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipient.id = :recipientId AND n.read = false")
    int markAllAsRead(@Param("recipientId") UUID recipientId);

    // 기준 시각보다 오래된 알림 삭제 (30일 보관 정책)
    long deleteByCreatedAtBefore(LocalDateTime createdAt);
}
