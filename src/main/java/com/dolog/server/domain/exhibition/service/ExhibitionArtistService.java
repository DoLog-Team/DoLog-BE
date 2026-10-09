package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistAddResponse;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistListResponse;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistStatusUpdateResponse;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistManageListResponse;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistGlobalListResponse;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionMyListResponse;
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
    ExhibitionArtistListResponse getArtistsByExhibition(
            UUID exhibitionId,
            String sort
    );
    void removeArtistFromExhibition(
            UUID accountId,
            UUID exhibitionId,
            UUID artistId
    );
    void leaveExhibition(UUID accountId, UUID exhibitionId);
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

    // 두록 어드민 전용. exhibitionId를 생략하면 전시를 가로질러 조회한다.
    ExhibitionArtistGlobalListResponse getArtistsForManagementAcrossExhibitions(
            UUID exhibitionId,
            ExhibitionArtistStatus status,
            String search,
            int page,
            int size
    );

    // 마이페이지 "내 전시 목록". status 생략 시 PENDING/JOINED만.
    ExhibitionMyListResponse getMyExhibitions(UUID accountId, ExhibitionArtistStatus status);
}
