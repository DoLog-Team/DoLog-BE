package com.dolog.server.domain.bts.entity;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "bts_artwork_map")
public class BtsArtworkMap extends BaseEntity {

    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bts_id")
    private Bts bts;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id")
    private Artwork artwork;
}
