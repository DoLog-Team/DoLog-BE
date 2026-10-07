package com.dolog.server.domain.artwork.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ArtworkDetailResponse {
    private String title;
    private String category;
    private String description;
    private String shortIntro;
    // 전시 항목 설정에서 숨긴 값은 null(목록은 빈 배열)로 내려간다.
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
    private String mainImage;
    private String locationMap;
    private long viewCount;
    private long likeCount;
    private boolean liked;
    private List<DetailImageInfo> detailImages;
    private List<ParticipantInfo> participants;

    private List<RelatedBtsInfo> relatedBts;

    // 동일 카테고리
    private List<RelatedArtworkInfo> sameCategoryArtworks;

    // 작품 둘러보기
    private List<RelatedArtworkInfo> alphabeticalArtworks;

    @Getter @Builder
    public static class DetailImageInfo {
        private UUID imageId;
        private String imageUrl;
        private String description;
    }

    @Getter @Builder
    public static class ParticipantInfo {
        private UUID artistId;
        private UUID profileId;
        private String nameKo;
        private String nameEn;
        private String profileImg;
        private String role;
        private String bio;
        private String email;
        private List<SnsInfo> sns;
    }

    @Getter @Builder
    public static class SnsInfo {
        private String platformName;
        private String url;
    }

    @Getter
    @Builder
    public static class RelatedBtsInfo {
        private UUID id;
        private String title;
        private String mainImg;
        private String author;
    }

    @Getter @Builder
    public static class RelatedArtworkInfo {
        private UUID id;
        private String title;
        private String category;
        private String artistName;
        private String mainImage;
        private String type;
    }
}