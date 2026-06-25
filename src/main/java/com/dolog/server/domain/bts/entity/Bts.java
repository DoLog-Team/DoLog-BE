package com.dolog.server.domain.bts.entity;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "bts")
public class Bts extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exhibition_id")
    private Exhibition exhibition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_profile_id")
    private ArtistProfile artistProfile;

    @Column(length = 255)
    private String title;

    @Column(name = "link_label", length = 255)
    private String linkLabel;

    @Column(name = "link_url", columnDefinition = "TEXT")
    private String linkUrl;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "main_img", columnDefinition = "TEXT")
    private String mainImg;

    @Builder.Default
    @OneToMany(mappedBy = "bts", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BtsArtworkMap> artworkMaps = new ArrayList<>();

    public void updateBtsInfo(String title, String linkLabel, String linkUrl, String content, String mainImg, ArtistProfile artistProfile) {
        if (title != null) this.title = title;
        if (linkLabel != null) this.linkLabel = linkLabel;
        if (linkUrl != null) this.linkUrl = linkUrl;
        if (content != null) this.content = content;
        if (mainImg != null) this.mainImg = mainImg;
        if (artistProfile != null) this.artistProfile = artistProfile;
    }
}
