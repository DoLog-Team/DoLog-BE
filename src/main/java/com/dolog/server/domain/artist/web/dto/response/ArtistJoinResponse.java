package com.dolog.server.domain.artist.web.dto.response;

import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;

import java.util.UUID;

public record ArtistJoinResponse(
        UUID exhibitionId,
        ExhibitionArtistStatus status
) {
}