package com.dolog.server.domain.artwork.web.dto.response;

import lombok.Builder;
import lombok.Getter;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ArtworkUpdateFullResponse {
    private UUID artworkId;
    private String title;
    private List<UUID> updatedArtistIds;
    private List<UUID> updatedImageIds;

    public static ArtworkUpdateFullResponse of(UUID artworkId, String title, List<UUID> artistIds, List<UUID> imageIds) {
        return ArtworkUpdateFullResponse.builder()
                .artworkId(artworkId)
                .title(title)
                .updatedArtistIds(artistIds)
                .updatedImageIds(imageIds)
                .build();
    }
}