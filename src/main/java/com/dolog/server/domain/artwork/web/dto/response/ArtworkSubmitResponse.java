package com.dolog.server.domain.artwork.web.dto.response;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;

import java.util.UUID;

public record ArtworkSubmitResponse(
        UUID artworkId,
        UUID exhibitionId,
        UUID zoneId,
        String locationMap,
        Integer orderIndex,
        ArtworkStatus status
) {
    public static ArtworkSubmitResponse from(Artwork artwork) {
        return new ArtworkSubmitResponse(
                artwork.getId(),
                artwork.getExhibition().getId(),
                artwork.getExhibitionZone().getId(),
                artwork.getLocationMap(),
                artwork.getOrderIndex(),
                artwork.getStatus()
        );
    }
}
