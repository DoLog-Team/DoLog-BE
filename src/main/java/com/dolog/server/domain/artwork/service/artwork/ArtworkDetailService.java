package com.dolog.server.domain.artwork.service.artwork;

import com.dolog.server.domain.artwork.web.dto.response.ArtworkDetailResponse;

import java.util.UUID;

public interface ArtworkDetailService {

    ArtworkDetailResponse getArtworkDetail(UUID exhibitionId, UUID artworkId);
}
