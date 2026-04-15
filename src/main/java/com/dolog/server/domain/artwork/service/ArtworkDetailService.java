package com.dolog.server.domain.artwork.service;

import com.dolog.server.domain.artwork.web.dto.response.ArtworkDetailResponse;

import java.util.UUID;

public interface ArtworkDetailService {
    // 이번에 구현할 상세 조회
    ArtworkDetailResponse getArtworkDetail(UUID exhibitionId, UUID artworkId);
}
