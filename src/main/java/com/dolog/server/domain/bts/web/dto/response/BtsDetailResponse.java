package com.dolog.server.domain.bts.web.dto.response;

import com.dolog.server.domain.artist.web.dto.response.ArtistSnsResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class BtsDetailResponse {
    private UUID btsId;
    private String title;
    private String mainImg;
    private String contentUrl;
    private List<BtsArtistProfileInfo> artists;
    private List<RelatedArtworkInfo> relatedArtworks;
    private List<RecommendedBtsInfo> recommendedBts;

    @Getter
    @Builder
    public static class BtsArtistProfileInfo {
        private UUID participantId; // ArtistProfile의 ID
        private String nameKo;
        private String nameEn;
        private String profileImage;
        private String bio;
        private ContactInfo contact;
    }

    @Getter
    @Builder
    public static class ContactInfo {
        private String email;
        private List<ArtistSnsResponse> sns;
    }

    @Getter
    @Builder
    public static class RelatedArtworkInfo {
        private UUID artworkId;
        private String title;
        private String image;
    }

    @Getter
    @Builder
    public static class RecommendedBtsInfo {
        private UUID btsId;
        private String title;
        private String mainImg;
    }
}
