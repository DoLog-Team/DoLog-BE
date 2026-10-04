package com.dolog.server.domain.artwork.entity;

import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.global.entity.BaseEntity;
import com.dolog.server.global.order.Orderable;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "artworks")
public class Artwork extends BaseEntity implements Orderable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exhibition_id")
    private Exhibition exhibition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private ExhibitionZone exhibitionZone;

    @Builder.Default
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "artwork", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<ArtworkImg> artworkImg = new ArrayList<>();

    @Column(length = 255)
    private String title;

    @Column(length = 100)
    private String category;

    @Column(length = 1000)
    private String material;

    private String size;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Lob
    @Column(name = "main_img", length = 700)
    private String mainImg;

    @Column(name = "location_map", length = 700)
    private String locationMap;

    @Column(name = "purchase_url")
    private String purchaseUrl;

    @Column(name = "youtube_url")
    private String youtubeUrl;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Column(name = "short_intro")
    private String shortIntro;

    @Column(precision = 10, scale = 2)
    private BigDecimal width;

    @Column(precision = 10, scale = 2)
    private BigDecimal height;

    @Column(precision = 10, scale = 2)
    private BigDecimal depth;

    @Column(name = "production_start_year")
    private Integer productionStartYear;

    @Column(name = "production_start_month")
    private Integer productionStartMonth;

    @Column(name = "production_start_day")
    private Integer productionStartDay;

    @Column(name = "production_end_year")
    private Integer productionEndYear;

    @Column(name = "production_end_month")
    private Integer productionEndMonth;

    @Column(name = "production_end_day")
    private Integer productionEndDay;

    @Column(name = "purchase_chat_url")
    private String purchaseChatUrl;

    @Column(name = "show_purchase_button")
    private Boolean showPurchaseButton;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ArtworkStatus status = ArtworkStatus.DRAFT;

    @Column(name = "hidden_at")
    private LocalDateTime hiddenAt;

    @Builder.Default
    @Column(name = "view_count", nullable = false)
    private long viewCount = 0L;

    @Builder.Default
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "artwork", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ArtworkArtistMap> artworkArtistMaps = new ArrayList<>();

    @Builder.Default
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "artwork", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<ArtworkMaterial> materials = new ArrayList<>();


    /**
     * 기본 정보 업데이트
     */
    public void updateBasicInfo(String title, String description, String purchaseUrl, String youtubeUrl) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (purchaseUrl != null) this.purchaseUrl = purchaseUrl;
        if (youtubeUrl != null) this.youtubeUrl = youtubeUrl;
    }

    /**
     * 모든 정보 업데이트
     */
    public void updateAllInfo(String title, String description, String category, ExhibitionZone exhibitionZone,
                              String material, String size, String mainImg, String locationMap,
                              String purchaseUrl, String youtubeUrl) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (category != null) this.category = category;
        if (exhibitionZone != null) this.exhibitionZone = exhibitionZone;
        if (material != null) this.material = material;
        if (size != null) this.size = size;
        if (mainImg != null) this.mainImg = mainImg;
        if (locationMap != null) this.locationMap = locationMap;
        if (purchaseUrl != null) this.purchaseUrl = purchaseUrl;
        if (youtubeUrl != null) this.youtubeUrl = youtubeUrl;
    }

    public void changeStatus(ArtworkStatus status) {
        this.status = status;
    }

    public void hide(LocalDateTime hiddenAt) {
        this.hiddenAt = hiddenAt;
    }

    public void unhide() {
        this.hiddenAt = null;
    }

    public boolean isHidden() {
        return hiddenAt != null;
    }

    // 두록 URL 은 숨김과 무관하게 PUBLISHED 면 노출, 전시 URL 은 숨김이면 비노출
    public boolean isVisibleOnDolog() {
        return status == ArtworkStatus.PUBLISHED;
    }

    public boolean isVisibleOnExhibition() {
        return isVisibleOnDolog() && !isHidden();
    }

    public void replaceMaterials(List<String> names) {
        this.materials.clear();
        for (int i = 0; i < names.size(); i++) {
            this.materials.add(ArtworkMaterial.builder()
                    .artwork(this)
                    .name(names.get(i))
                    .orderIndex(i + 1)
                    .build());
        }
    }


    // 순서 정렬
    @Override
    public Integer getOrderIndex() {
        return this.orderIndex;
    }

    @Override
    public void updateOrder(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public void updateZone(ExhibitionZone zone) {
        this.exhibitionZone = zone;
    }

    public void cancelExhibitionSubmission() {
        this.exhibition = null;
        this.exhibitionZone = null;
    }
}
