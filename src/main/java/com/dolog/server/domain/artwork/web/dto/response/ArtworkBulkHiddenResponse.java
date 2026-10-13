package com.dolog.server.domain.artwork.web.dto.response;

import java.util.List;

public record ArtworkBulkHiddenResponse(
        boolean hidden,
        int changedCount,
        List<ArtworkHiddenResponse> artworks
) {}
