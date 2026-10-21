package com.dolog.server.domain.artwork.web.dto.response;

import java.util.UUID;

public record ArtworkExhibitionOnlyResponse(
        UUID artworkId,
        boolean exhibitionOnly
) {}
