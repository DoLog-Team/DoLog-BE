package com.dolog.server.domain.artwork.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ArtworkDetailResponse {
    private String title;
    private String category;
    private String material;
    private String size;
    private String description;
    private String purchaseUrl;
    private String mainImage;
    private ZoneInfo zone;
    private List<DetailImageInfo> detailImages;
    private List<ParticipantInfo> participants;
    private List<RelatedBtsInfo> relatedBts;

    @Getter @Builder
    public static class ZoneInfo {
        private UUID id;
        private String name;
        private String mapImage;
    }

    @Getter @Builder
    public static class DetailImageInfo {
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
    }
}