package com.dolog.server.domain.artwork.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class ArtworkArtistMappingRequest {
    private UUID artistProfileId;

    private String artistRole;
}