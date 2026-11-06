package com.dolog.server.domain.artwork.entity;

import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

// 작가 등록 폼 "재료 및 기법" 자동완성용 기본 목록. ArtworkMaterial(작품별 실제 입력값)과는 별개고,
// 여기 값은 "검색/선택 후보"일 뿐 작품에 저장된 재료 이름을 강제하지 않는다. (ArtworkCategory와 같은 구조)
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "material_options")
public class ArtworkMaterialOption extends BaseEntity {

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
