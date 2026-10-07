package com.dolog.server.domain.artwork.web.dto.response;

import java.util.UUID;

// counted: 이번 요청으로 조회수가 올랐는지 (이 방문자가 이미 본 작품이면 false)
public record ArtworkViewResponse(
        UUID artworkId,
        long viewCount,
        boolean counted,
        String visitorId
) {
}
