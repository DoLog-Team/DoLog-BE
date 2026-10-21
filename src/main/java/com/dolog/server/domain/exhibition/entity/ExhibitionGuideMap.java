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
@Table(name = "exhibition_guide_maps")
public class ExhibitionGuideMap extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exhibition_id", nullable = false)
    private Exhibition exhibition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private ExhibitionZone zone;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "description")
    private String description;

    // 전시 안에서의 노출 순서(1부터). 어드민이 reorder API로 변경한다.
    @Column(name = "order_index")
    private Integer orderIndex;

    public void updateOrder(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public void updateGuideMap(ExhibitionZone zone, String imageUrl, String description) {
        this.zone = zone;
        this.imageUrl = imageUrl;
        this.description = description;
    }
}