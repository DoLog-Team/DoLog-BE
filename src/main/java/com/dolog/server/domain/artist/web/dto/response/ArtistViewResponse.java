package com.dolog.server.domain.artist.web.dto.response;

import java.util.UUID;

public record ArtistViewResponse(
        UUID artistId,
        long viewCount,
        boolean counted,
        String visitorId
) {
}
