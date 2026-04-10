package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistAddResponse;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistListResponse;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistRemoveResponse;

import java.util.List;
import java.util.UUID;

public interface ExhibitionArtistService {
    // 전시 <-> 작가 추가, 삭제, 조회
    ExhibitionArtistAddResponse addArtistToExhibition(UUID exhibitionId, UUID artistId);
    List<ExhibitionArtistListResponse> getArtistsByExhibition(UUID exhibitionId);
    ExhibitionArtistRemoveResponse removeArtistFromExhibition(UUID exhibitionId, UUID artistId);

}
