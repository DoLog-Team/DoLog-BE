package com.dolog.server.domain.exhibition.web.dto.response.artist;

import java.util.List;

public record ExhibitionArtistGlobalListResponse(
        List<ExhibitionArtistGlobalItemResponse> artists,
        int totalCount,
        int totalPages
) {
}
