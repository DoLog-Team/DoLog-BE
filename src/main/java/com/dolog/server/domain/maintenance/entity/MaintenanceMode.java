package com.dolog.server.domain.maintenance.entity;

import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "maintenance_mode")
public class MaintenanceMode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    public void start(LocalDateTime startAt, LocalDateTime endAt) {
        this.isActive = true;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public void end() {
        this.isActive = false;
    }
}
