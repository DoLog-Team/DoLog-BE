package com.dolog.server.domain.artwork.web.dto.response;

import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;

import java.util.UUID;

public record ArtworkStatusResponse(
        UUID artworkId,
        ArtworkStatus status
) {}
