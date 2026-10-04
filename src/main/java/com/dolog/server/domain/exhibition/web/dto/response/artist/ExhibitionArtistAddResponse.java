package com.dolog.server.domain.exhibition.web.dto.response.artist;

import com.dolog.server.domain.exhibition.entity.ExhibitionArtistMap;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;

import java.util.UUID;

public record ExhibitionArtistAddResponse(
        UUID exhibitionArtistId,
        ExhibitionArtistStatus status
) {
    public static ExhibitionArtistAddResponse from(
            ExhibitionArtistMap exhibitionArtist
    ) {
        return new ExhibitionArtistAddResponse(
                exhibitionArtist.getId(),
                exhibitionArtist.getStatus()
        );
    }
}
