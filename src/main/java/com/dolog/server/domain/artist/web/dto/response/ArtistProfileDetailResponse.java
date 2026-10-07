package com.dolog.server.domain.artist.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtistProfileDetailResponse {
    private UUID profileId;
    private UUID artistId;
    private UUID exhibitionId;
    private String nameKo;
    private String nameEn;
    private String bio;
    private String profileImg;
    private String email;
    private List<SnsInfo> snsList;
    private String purchaseContactUrl;
    private List<ArtworkSummary> artworks;
    private NeighborArtist prevArtist;
    private NeighborArtist nextArtist;
    private int likeCount;
    private boolean liked;
    private long viewCount;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SnsInfo {
        private UUID snsId;
        private String platformName;
        private String url;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArtworkSummary {
        private UUID artworkId;
        private String title;
        private String mainImg;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NeighborArtist {
        private UUID profileId;
        private String nameKo;
        private String profileImg;
    }
}
