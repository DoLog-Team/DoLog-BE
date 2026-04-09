package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.web.dto.response.ExhibitionArtistAddResponse;
import com.dolog.server.domain.exhibition.web.dto.response.ExhibitionArtistListResponse;
import com.dolog.server.domain.exhibition.web.dto.response.ExhibitionArtistRemoveResponse;

import java.util.List;
import java.util.UUID;

public interface ExhibitionArtistService {
    // 전시 <-> 작가 추가, 삭제, 조회
    ExhibitionArtistAddResponse addArtistToExhibition(UUID exhibitionId, UUID artistId);
    List<ExhibitionArtistListResponse> getArtistsByExhibition(UUID exhibitionId);
    ExhibitionArtistRemoveResponse removeArtistFromExhibition(UUID exhibitionId, UUID artistId);

}
