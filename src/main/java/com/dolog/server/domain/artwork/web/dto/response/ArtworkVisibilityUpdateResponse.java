package com.dolog.server.domain.artwork.web.dto.response;

import com.dolog.server.domain.artwork.entity.enums.ArtworkVisibilityOption;

import java.util.UUID;

public record ArtworkVisibilityUpdateResponse(
        UUID artworkId,
        ArtworkVisibilityOption visibility
) {}
