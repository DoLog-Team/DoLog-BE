package com.dolog.server.domain.artwork.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtworkCreateResponse {
    private UUID artworkId;

    public static ArtworkCreateResponse from(UUID id) {
        return ArtworkCreateResponse.builder()
                .artworkId(id)
                .build();
    }
}