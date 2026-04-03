package com.dolog.server.domain.artist.entity;

import com.dolog.server.domain.exhibition.entity.Exhibition;
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
        name = "artist_profiles",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"artist_id", "exhibition_id"})
        }
)
public class ArtistProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    private Artist artist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exhibition_id", nullable = false)
    private Exhibition exhibition;

    @Column(name = "name_ko", length = 100)
    private String nameKo;

    @Column(name = "name_en", length = 100)
    private String nameEn;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    private String email;

    @Column(name = "profile_img")
    private String profileImg;

    public void updateProfile(String nameKo, String nameEn, String bio, String email, String profileImg) {
        this.nameKo = nameKo;
        this.nameEn = nameEn;
        this.bio = bio;
        this.email = email;
        this.profileImg = profileImg;
    }

    public void togglePublicStatus(boolean isPublic) {
        this.isPublic = isPublic;
    }
}
