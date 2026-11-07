package com.dolog.server.domain.artwork.web.dto.response;

import com.dolog.server.domain.artwork.entity.enums.ArtworkSizeType;
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
    // null이면 사이즈 미입력. ROUND는 width가 지름, VARIABLE은 숫자 없이 "가변크기"로 표시한다.
    private ArtworkSizeType sizeType;
    // 영상 길이(초)
    private Integer durationSeconds;
    private Integer productionStartYear;
    private Integer productionStartMonth;
    private Integer productionStartDay;
    private Integer productionEndYear;
    private Integer productionEndMonth;
    private Integer productionEndDay;
    private String mainImage;
    private String locationMap;
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
                .sizeType(artwork.getSizeType())
                .durationSeconds(artwork.getDurationSeconds())
                .productionStartYear(artwork.getProductionStartYear())
                .productionStartMonth(artwork.getProductionStartMonth())
                .productionStartDay(artwork.getProductionStartDay())
                .productionEndYear(artwork.getProductionEndYear())
                .productionEndMonth(artwork.getProductionEndMonth())
                .productionEndDay(artwork.getProductionEndDay())
                .mainImage(artwork.getMainImg())
                .locationMap(artwork.getLocationMap())
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
