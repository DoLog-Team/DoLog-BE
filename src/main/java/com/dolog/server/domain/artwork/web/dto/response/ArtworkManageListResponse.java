package com.dolog.server.domain.artwork.web.dto.response;

import java.util.List;

public record ArtworkManageListResponse(
        List<ArtworkManageItemResponse> artworks,
        int totalCount,
        int totalPages,
        int planLimitExceededCount
) {
}
