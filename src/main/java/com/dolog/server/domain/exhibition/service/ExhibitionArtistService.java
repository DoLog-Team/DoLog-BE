package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistAddResponse;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistListResponse;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistStatusUpdateResponse;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistManageListResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistJoinCodeValidateResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistJoinResponse;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;

import java.util.List;
import java.util.UUID;

public interface ExhibitionArtistService {
    // 전시 <-> 작가 추가, 삭제, 조회
    ExhibitionArtistAddResponse addArtistToExhibition(
            UUID accountId,
            UUID exhibitionId,
            UUID artistId
    );
    List<ExhibitionArtistListResponse> getArtistsByExhibition(UUID exhibitionId, String sort);
    void removeArtistFromExhibition(
            UUID accountId,
            UUID exhibitionId,
            UUID artistId
    );
    ArtistJoinCodeValidateResponse validateJoinCode(UUID accountId, String joinCode);
    ArtistJoinResponse joinExhibition(UUID accountId, String joinCode, String greeting);
    ExhibitionArtistStatusUpdateResponse updateArtistStatuses(
            UUID accountId,
            UUID exhibitionId,
            List<UUID> artistIds,
            ExhibitionArtistStatus status
    );

    ExhibitionArtistManageListResponse getArtistsForManagement(
            UUID accountId,
            UUID exhibitionId,
            ExhibitionArtistStatus status,
            String search,
            int page,
            int size
    );
}
