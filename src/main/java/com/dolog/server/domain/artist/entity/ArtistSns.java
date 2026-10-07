package com.dolog.server.domain.artist.entity;

import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "artist_sns")
public class ArtistSns extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_profile_id", nullable = false)
    private ArtistProfile artistProfile;

    @Column(name = "platform_name", nullable = false, length = 100)
    private String platformName;

    @Column(nullable = false)
    private String url;

    public void update(String platformName, String url) {
        if (platformName != null) {
            this.platformName = platformName.trim();
        }
        if (url != null) {
            this.url = url.trim();
        }
    }
}
