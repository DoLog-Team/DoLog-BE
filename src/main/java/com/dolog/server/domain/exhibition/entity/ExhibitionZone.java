package com.dolog.server.domain.exhibition.entity;

import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "exhibition_zones")
public class ExhibitionZone extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exhibition_id")
    private Exhibition exhibition;

    @Column(length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "order_id")
    private Integer orderId;

    // true면 사용자 화면에서 이 그룹의 작품이 숨겨진다
    @Column(name = "is_hidden", nullable = false)
    private boolean hidden;

    public void update(String name, String description, Integer orderId) {
        if (name != null) this.name = name;
        if (description != null) this.description = description;
        if (orderId != null) this.orderId = orderId;
    }

    public void changeHidden(boolean hidden) {
        this.hidden = hidden;
    }

    // 일괄 저장용: null 값도 그대로 반영한다 (설명을 비우면 null로 저장)
    public void replace(String name, String description, Integer orderId) {
        this.name = name;
        this.description = description;
        this.orderId = orderId;
    }
}
