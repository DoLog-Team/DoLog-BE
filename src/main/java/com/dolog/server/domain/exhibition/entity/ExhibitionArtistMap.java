package com.dolog.server.domain.exhibition.entity;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
        name = "exhibition_artist_map",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"exhibition_id", "artist_id"})
        })
public class ExhibitionArtistMap extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exhibition_id")
    private Exhibition exhibition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id")
    private Artist artist;

    @Enumerated(EnumType.STRING)
    private ExhibitionArtistStatus status;

    public void updateStatus(ExhibitionArtistStatus status) {
        this.status = status;
    }
}
