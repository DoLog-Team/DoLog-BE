package com.dolog.server.domain.artwork.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class CategoryArtworkResponse {
    private String categoryName;
    private List<SimpleArtworkResponse> artworks;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SimpleArtworkResponse {
        private UUID id;
        private String title;
        private String imageUrl;
        private UUID exhibitionId;
        private String slug;
        private String exhibitionTitle;
        private String artistName;
    }
}
