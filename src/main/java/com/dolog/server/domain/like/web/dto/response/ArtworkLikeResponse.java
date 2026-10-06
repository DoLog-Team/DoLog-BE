package com.dolog.server.domain.like.web.dto.response;

import java.util.UUID;

public record ArtworkLikeResponse(
        UUID artworkId,
        String visitorId,
        long likeCount,
        boolean liked
) {}
