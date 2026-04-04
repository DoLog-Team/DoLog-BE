package com.dolog.server.domain.exhibition.web.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ExhibitionArtistListResponse {

    private UUID artistId;
    private String artistName;

    public static ExhibitionArtistListResponse from(UUID artistId, String artistName) {
        return ExhibitionArtistListResponse.builder()
                .artistId(artistId)
                .artistName(artistName)
                .build();
    }
}