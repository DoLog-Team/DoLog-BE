package com.dolog.server.domain.exhibition.web.dto.response.artwork;

import com.dolog.server.domain.artwork.web.dto.response.CategoryArtworkResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ExhibitionArtworkListResponse {
    private UUID exhibitionId;

    private List<MapInfo> maps;
    private List<ZoneInfo> zones;

    @Getter
    @Builder
    public static class ZoneInfo {
        private String zoneName;
        private String description;
        private Integer zoneOrderId; // 피드백: 존 순서
        private List<CategoryArtworkResponse> categories; // 기존 CategoryArtworkResponse 재사용
    }

    @Getter @Builder
    public static class MapInfo {
        private UUID id;
        private String imageUrl;
        private String description;
    }

    @Getter @Builder
    public static class CategoryInfo {
        private String categoryName;
        private List<ArtworkInfo> artworks; // 카테고리 안에 드디어 작품 리스트!
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
