package com.dolog.server.domain.artwork.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

// artwork_materials 에는 updated_at 이 없어서 BaseEntity 를 상속하지 않는다.
@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "artwork_materials")
public class ArtworkMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id", nullable = false)
    private Artwork artwork;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(name = "order_index")
    private Integer orderIndex;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
