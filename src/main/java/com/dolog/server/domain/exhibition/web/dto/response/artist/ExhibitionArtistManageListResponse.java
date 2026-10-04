package com.dolog.server.domain.exhibition.web.dto.response.artist;

import java.util.List;

public record ExhibitionArtistManageListResponse(
        List<ExhibitionArtistManageItemResponse> artists,
        long totalElements,
        int totalPages
) {
}