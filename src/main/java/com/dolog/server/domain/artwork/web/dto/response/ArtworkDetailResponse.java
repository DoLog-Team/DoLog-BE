package com.dolog.server.domain.artwork.web.dto.response;

import com.dolog.server.domain.artwork.entity.enums.ArtworkSizeType;
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
    // "관련 전시 보러가기" FAB용. 단독(미출품) 작품이면 셋 다 null.
    private UUID exhibitionId;
    private String exhibitionTitle;
    private String slug;
    private UUID zoneId;
    private String zoneName;
    private String description;
    private String shortIntro;
    // 전시 항목 설정에서 숨긴 값은 null(목록은 빈 배열)로 내려간다.
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

    // 동일 구역(zone)
    private List<RelatedArtworkInfo> sameZoneArtworks;

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