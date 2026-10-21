package com.dolog.server.domain.artwork.web.dto.response;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.entity.ArtworkImg;
import com.dolog.server.domain.artwork.entity.ArtworkMaterial;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// 작가 본인의 "내 작품 상세 조회 (편집용)". 공개 여부/숨김과 무관하게 전체 필드를 돌려준다.
@Getter
@Builder
public class ArtworkEditResponse {

    private UUID artworkId;
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
    private String purchaseUrl;
    private String purchaseChatUrl;
    private Boolean showPurchaseButton;
    private String youtubeUrl;
    private String mainImg;
    private ArtworkStatus status;
    private boolean hidden;
    private boolean exhibitionOnly;
    private UUID exhibitionId;
    private String exhibitionName;
    private UUID zoneId;
    private String zoneName;
    private List<ImageInfo> images;
    private List<ArtistInfo> artists;

    public static ArtworkEditResponse of(Artwork artwork, String exhibitionName) {
        return ArtworkEditResponse.builder()
                .artworkId(artwork.getId())
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
                .purchaseUrl(artwork.getPurchaseUrl())
                .purchaseChatUrl(artwork.getPurchaseChatUrl())
                .showPurchaseButton(artwork.getShowPurchaseButton())
                .youtubeUrl(artwork.getYoutubeUrl())
                .mainImg(artwork.getMainImg())
                .status(artwork.getStatus())
                .hidden(artwork.isHidden())
                .exhibitionOnly(artwork.isExhibitionOnly())
                .exhibitionId(artwork.getExhibition() != null ? artwork.getExhibition().getId() : null)
                .exhibitionName(exhibitionName)
                .zoneId(artwork.getExhibitionZone() != null ? artwork.getExhibitionZone().getId() : null)
                .zoneName(artwork.getExhibitionZone() != null ? artwork.getExhibitionZone().getName() : null)
                .images(artwork.getArtworkImg().stream().map(ImageInfo::from).toList())
                .artists(artwork.getArtworkArtistMaps().stream().map(ArtistInfo::from).toList())
                .build();
    }

    @Getter
    @Builder
    public static class ImageInfo {
        private UUID imageId;
        private String imageUrl;
        private String description;
        private Integer orderIndex;

        public static ImageInfo from(ArtworkImg img) {
            return ImageInfo.builder()
                    .imageId(img.getId())
                    .imageUrl(img.getImageUrl())
                    .description(img.getDescription())
                    .orderIndex(img.getOrderIndex())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ArtistInfo {
        private UUID artistId;
        private String nameKo;
        private String role;

        public static ArtistInfo from(ArtworkArtistMap map) {
            Artist artist = map.getArtist();
            return ArtistInfo.builder()
                    .artistId(artist.getId())
                    .nameKo(artist.getNameKo())
                    .role(map.getArtistRole())
                    .build();
        }
    }
}
