package com.dolog.server.domain.artwork.entity;

import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

// 작가 등록 폼/두록 공개 필터용 작품 카테고리 드롭다운 목록. Artwork.category는 자유 문자열로 그대로 두고
// FK/enum 검증은 하지 않는다 — 여기 값은 "지금 보여줄 목록"일 뿐, 이미 저장된 작품의 category 값을 강제하지 않는다.
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "artwork_categories")
public class ArtworkCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    public void updateName(String name) {
        this.name = name;
    }

    public void updateOrder(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }
}
