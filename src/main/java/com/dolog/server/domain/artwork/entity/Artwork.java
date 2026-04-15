package com.dolog.server.domain.artwork.entity;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "artworks")
public class Artwork extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exhibition_id", nullable = false)
    private Exhibition exhibition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private ExhibitionZone exhibitionZone;

    @Builder.Default // Builder 사용 시 기본값으로 초기화되도록 설정
    @BatchSize(size = 100) // ⭐️ 추가
    @OneToMany(mappedBy = "artwork", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArtworkImg> artworkImg = new ArrayList<>();

    @Column(length = 255)
    private String title;

    @Column(length = 100)
    private String category;

    private String material;

    private String size;

    @Lob
    @Column
    private String description;

    @Lob
    @Column(name = "main_img")
    private String mainImg;

    @Column(name = "purchase_url")
    private String purchaseUrl;

    @Column(name = "zone", length = 50)
    private String zone;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Builder.Default
    @BatchSize(size = 100) // ⭐️ 추가
    @OneToMany(mappedBy = "artwork", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArtworkArtistMap> artworkArtistMaps = new ArrayList<>();


    public void updateBasicInfo(String title, String description, String purchaseUrl) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (purchaseUrl != null) this.purchaseUrl = purchaseUrl;
    }

    public void updateAllInfo(String title, String description, String category, ExhibitionZone exhibitionZone, String material, String size, String mainImg, String purchaseUrl) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (category != null) this.category = category;
        if (exhibitionZone != null) this.exhibitionZone = exhibitionZone;
        if (material != null) this.material = material;
        if (size != null) this.size = size;
        if (mainImg != null) this.mainImg = mainImg;
        if (purchaseUrl != null) this.purchaseUrl = purchaseUrl;
    }
}
