package com.dolog.server.domain.notification.entity;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.notification.entity.enums.NotificationType;
import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    // 문구에 쓰일 값 (예: exhibitionName, zoneName). 문구 자체는 프론트에서 만든다
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "JSON")
    private Map<String, String> payload;

    // 관련 대상 ID (전시, 작가 등). 종류에 따라 없을 수 있음
    @Column(name = "reference_id", columnDefinition = "BINARY(16)")
    private UUID referenceId;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    public void markAsRead() {
        this.read = true;
    }
}
