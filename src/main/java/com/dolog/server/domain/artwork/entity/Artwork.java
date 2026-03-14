package com.dolog.server.domain.artwork.entity;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(nullable = false)
    private String title;

    @Column(length = 100)
    private String category;

    private String material;

    private String size;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String zone;

    @Column(name = "main_img", columnDefinition = "TEXT")
    private String mainImg;

    @Column(name = "purchase_url")
    private String purchaseUrl;

    public void updateArtwork(String title, String category, String material, String size, String description, String mainImg) {
        this.title = title;
        this.category = category;
        this.material = material;
        this.size = size;
        this.description = description;
        this.mainImg = mainImg;
    }

    public void updateZone(ExhibitionZone zone) {
        this.exhibitionZone = zone;
    }
}
