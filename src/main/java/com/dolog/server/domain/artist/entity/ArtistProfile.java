package com.dolog.server.domain.artist.entity;

import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
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

    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;

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

    @Column(name = "purchase_contact_url", length = 255)
    private String purchaseContactUrl;

    @Builder.Default
    @Column(name = "view_count", nullable = false)
    private long viewCount = 0L;

    @Builder.Default
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "artistProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArtistSns> snsList = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "artistProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArtworkArtistMap> artworkArtistMaps = new ArrayList<>();

    public void updateProfile(
            String nameKo,
            String nameEn,
            String bio,
            String email,
            String purchaseContactUrl,
            Boolean isPublic,
            String profileImg
    ) {
        // 값이 존재할 때만 업데이트 (기존 값 유지)
        if (nameKo != null && !nameKo.isBlank()) {
            this.nameKo = nameKo.trim();
        }
        if (nameEn != null) {
            this.nameEn = normalizeNullableText(nameEn);
        }
        if (bio != null) {
            this.bio = normalizeNullableText(bio);
        }
        if (email != null) {
            this.email = normalizeNullableText(email);
        }
        if (purchaseContactUrl != null) {
            this.purchaseContactUrl = normalizeNullableText(
                    purchaseContactUrl
            );
        }
        if (isPublic != null) {
            this.isPublic = isPublic;
        }
        if (profileImg != null) {
            this.profileImg = profileImg;
        }
    }

    private String normalizeNullableText(String value) {
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public void clearProfileImg() {
        this.profileImg = null;
    }

    // 처음 등록할 때 Artist의 기본 정보를 자동으로 채워줌
    public void fillDefaultInfoFromArtist() {
        if (this.nameKo == null || this.nameKo.isBlank()) {
            this.nameKo = this.artist.getNameKo();
        }
        if (this.nameEn == null || this.nameEn.isBlank()) {
            this.nameEn = this.artist.getNameEn();
        }
        // 이메일 자동 채우기 추가
        if (this.email == null || this.email.isBlank()) {
            if (this.artist.getAccount() != null) {
                this.email = this.artist.getAccount().getEmail();
            }
        }
    }

    public void togglePublicStatus(boolean isPublic) {
        this.isPublic = isPublic;
    }

}
