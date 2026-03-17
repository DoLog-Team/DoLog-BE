package com.dolog.server.domain.artwork.entity;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "artwork_artist_maps")
public class ArtworkArtistMap extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id", nullable = false)
    private Artwork artwork;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    @Column(name = "artist_role", length = 100, nullable = false)
    private String artistRole;

    public void updateRole(String artistRole) {
        if (artistRole != null) {
            this.artistRole = artistRole;
        }
    }
}
