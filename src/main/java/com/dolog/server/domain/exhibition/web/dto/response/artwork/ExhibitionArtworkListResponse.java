package com.dolog.server.domain.exhibition.web.dto.response.artwork;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ExhibitionArtworkListResponse {
    private UUID exhibitionId;

    private List<MapInfo> maps;
    private List<ArtworkInfo> artworks;

    @Getter @Builder
    public static class MapInfo {
        private UUID id;
        private String imageUrl;
        private String description;
    }

    @Getter @Builder
    public static class ArtworkInfo {
        private UUID artworkId;
        private String title;
        private String category;
        private String zone;
        private String mainImage;
        private List<ArtistInfo> artists;
    }

    @Getter @Builder
    public static class ArtistInfo {
        private UUID id;
        private String name;
    }
}
