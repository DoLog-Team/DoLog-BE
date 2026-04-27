package com.dolog.server.domain.artwork.service;

import com.dolog.server.domain.exhibition.web.dto.response.artwork.ExhibitionArtworkListResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ArtworkExhibitionService {
    // 1. 전시회별 작품 목록 조회 (기존 getExhibitionArtworkList)
    ExhibitionArtworkListResponse getExhibitionArtworkList(UUID exhibitionId, String zone, String category);

    // 2. 전시회 제목 일괄 조회 (N+1 방지용)
    Map<UUID, String> fetchExhibitionDetailMap(List<UUID> exhibitionIds);
}
