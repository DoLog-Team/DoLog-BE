package com.dolog.server.domain.like.web.dto.response;

import java.util.UUID;

public record ArtistProfileLikeResponse(
        UUID profileId,
        String visitorId,
        long likeCount,
        boolean liked
) {}
