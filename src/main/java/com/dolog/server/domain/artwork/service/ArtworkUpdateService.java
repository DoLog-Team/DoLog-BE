package com.dolog.server.domain.artwork.service;

import com.dolog.server.domain.artwork.web.dto.request.ArtworkUpdateFullRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkUpdateRequest;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkCreateResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkUpdateFullResponse;

import java.util.UUID;

public interface ArtworkUpdateService {
    ArtworkCreateResponse updateArtwork(UUID artworkId, ArtworkUpdateRequest request);
    ArtworkUpdateFullResponse updateArtworkFull(UUID exhibitionId, UUID artworkId, ArtworkUpdateFullRequest request);
}
