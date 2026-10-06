package com.dolog.server.domain.artwork.web.dto.response;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkMaterial;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ArtworkCreateResponse {
    private UUID id;
    private String title;
    private String category;
    private String description;
    private String shortIntro;
    private List<String> materials;
    private BigDecimal width;
    private BigDecimal height;
    private BigDecimal depth;
    private Integer productionStartYear;
    private Integer productionStartMonth;
    private Integer productionStartDay;
    private Integer productionEndYear;
    private Integer productionEndMonth;
    private Integer productionEndDay;
    private String mainImage;
    private String purchaseUrl;
    private String purchaseChatUrl;
    private Boolean showPurchaseButton;
    private String youtubeUrl;
    private ArtworkStatus status;
    private UUID exhibitionId;
    private UUID zoneId;
    private String artistName;

    public static ArtworkCreateResponse of(Artwork artwork, String artistName) {
        return ArtworkCreateResponse.builder()
                .id(artwork.getId())
                .title(artwork.getTitle())
                .category(artwork.getCategory())
                .description(artwork.getDescription())
                .shortIntro(artwork.getShortIntro())
                .materials(artwork.getMaterials().stream().map(ArtworkMaterial::getName).toList())
                .width(artwork.getWidth())
                .height(artwork.getHeight())
                .depth(artwork.getDepth())
                .productionStartYear(artwork.getProductionStartYear())
                .productionStartMonth(artwork.getProductionStartMonth())
                .productionStartDay(artwork.getProductionStartDay())
                .productionEndYear(artwork.getProductionEndYear())
                .productionEndMonth(artwork.getProductionEndMonth())
                .productionEndDay(artwork.getProductionEndDay())
                .mainImage(artwork.getMainImg())
                .purchaseUrl(artwork.getPurchaseUrl())
                .purchaseChatUrl(artwork.getPurchaseChatUrl())
                .showPurchaseButton(artwork.getShowPurchaseButton())
                .youtubeUrl(artwork.getYoutubeUrl())
                .status(artwork.getStatus())
                .exhibitionId(artwork.getExhibition() != null ? artwork.getExhibition().getId() : null)
                .zoneId(artwork.getExhibitionZone() != null ? artwork.getExhibitionZone().getId() : null)
                .artistName(artistName)
                .build();
    }
}
