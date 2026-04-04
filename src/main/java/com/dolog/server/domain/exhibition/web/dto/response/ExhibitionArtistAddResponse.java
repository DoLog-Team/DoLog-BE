package com.dolog.server.domain.exhibition.web.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ExhibitionArtistAddResponse {

    private UUID exhibitionId;
    private String exhibitionName;

    private UUID artistId;
    private String artistName;

    public static ExhibitionArtistAddResponse of(
            UUID exhibitionId,
            String exhibitionName,
            UUID artistId,
            String artistName
    ) {
        return ExhibitionArtistAddResponse.builder()
                .exhibitionId(exhibitionId)
                .exhibitionName(exhibitionName)
                .artistId(artistId)
                .artistName(artistName)
                .build();
    }
}