package com.dolog.server.domain.artwork.service;

import com.dolog.server.domain.artwork.web.dto.request.*;
import com.dolog.server.domain.artwork.web.dto.response.*;
import com.dolog.server.domain.exhibition.web.dto.response.artwork.ExhibitionArtworkListResponse;

import java.util.List;
import java.util.UUID;

public interface ArtworkService {
    Object getArtworks(Boolean main, String category, String search, String sort);

    ArtworkCreateResponse createArtwork(UUID accountId, ArtworkCreateRequest request);

    ArtworkImgCreateResponse createArtworkImages(UUID accountId, boolean isDologAdmin, UUID artworkId, List<ArtworkImgCreateRequest> requests);

    ArtworkCreateResponse updateArtwork(UUID accountId, UUID artworkId, ArtworkUpdateRequest request);

    void deleteArtwork(UUID accountId, UUID artworkId);

    ArtworkSubmitResponse submitArtwork(UUID accountId, UUID artworkId, ArtworkSubmitRequest request);

    void cancelSubmission(UUID accountId, UUID artworkId);

    ArtworkExhibitionStatusResponse getSubmissionStatus(UUID accountId, UUID artworkId);

    ArtworkStatusResponse changeArtworkStatus(UUID accountId, UUID artworkId, ArtworkStatusUpdateRequest request);

    ArtworkHiddenResponse changeArtworkHidden(UUID accountId, UUID artworkId, ArtworkHiddenUpdateRequest request);

    void reorderArtwork(UUID artworkId, Integer prev, Integer next);
    void moveArtworkZone(UUID artworkId, UUID zoneId, Integer prev, Integer next);

    ArtworkImgUpdateResponse updateArtworkImage(UUID accountId, boolean isDologAdmin, UUID artworkId, UUID imageId, ArtworkImgUpdateRequest request);

    void deleteArtworkImage(UUID accountId, boolean isDologAdmin, UUID artworkId, UUID imageId);

    ArtworkArtistMappingResponse createArtistMapping(UUID accountId, UUID artworkId, ArtworkArtistMappingRequest request);

    ArtworkArtistMappingResponse updateArtistMapping(UUID accountId, UUID artworkId, UUID artistId, ArtworkArtistRoleRequest request);

    void deleteArtistMapping(UUID accountId, UUID artworkId, UUID artistId);

    ExhibitionArtworkListResponse getExhibitionArtworkList(UUID exhibitionId, String zone, String category, String search);

    /**
     * 작품 전체 정보 수정 (PUT)
     * 작품 기본 정보, 작가 매핑, 상세 이미지 리스트를 한꺼번에 동기화합니다.
     */
    ArtworkUpdateFullResponse updateArtworkFull(UUID exhibitionId, UUID artworkId, ArtworkUpdateFullRequest request);
}
