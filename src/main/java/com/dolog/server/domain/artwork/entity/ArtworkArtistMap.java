package com.dolog.server.domain.artwork.entity;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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


    // TODO: 이거 빼야할 듯
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_profile_id")
    private ArtistProfile artistProfile;

    @Column(name = "artist_role", length = 100, nullable = false)
    private String artistRole;

    public void updateRole(String artistRole) {
        if (artistRole != null) {
            this.artistRole = artistRole;
        }
    }

    public void updateArtistProfile(Artist artist, ArtistProfile profile, String role) {
        this.artist = artist;
        this.artistProfile = profile;
        if (role != null) this.artistRole = role;
    }
}
