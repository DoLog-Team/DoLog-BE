package com.dolog.server.domain.artwork.service.artwork;

import com.dolog.server.domain.artwork.web.dto.request.*;
import com.dolog.server.domain.artwork.web.dto.response.*;
import com.dolog.server.domain.exhibition.web.dto.response.artwork.ExhibitionArtworkListResponse;

import java.util.List;
import java.util.UUID;

public interface ArtworkService {
    Object getArtworks(Boolean main, String category, String search);

    ArtworkCreateResponse createArtwork(ArtworkCreateRequest request);

    ArtworkImgCreateResponse createArtworkImages(UUID artworkId, List<ArtworkImgCreateRequest> requests);

    ArtworkCreateResponse updateArtwork(UUID artworkId, ArtworkUpdateRequest request);

    void deleteArtwork(UUID artworkId);

    ArtworkImgUpdateResponse updateArtworkImage(UUID artworkId, UUID imageId, ArtworkImgUpdateRequest request);

    void deleteArtworkImage(UUID artworkId, UUID imageId);

    ArtworkArtistMappingResponse createArtistMapping(UUID artworkId, ArtworkArtistMappingRequest request);

    ArtworkArtistMappingResponse updateArtistMapping(UUID artworkId, UUID artistId, ArtworkArtistMappingRequest request);

    void deleteArtistMapping(UUID artworkId, UUID artistId);

    ExhibitionArtworkListResponse getExhibitionArtworkList(UUID exhibitionId, String zone, String category, String search);

    /**
     * 작품 전체 정보 수정 (PUT)
     * 작품 기본 정보, 작가 매핑, 상세 이미지 리스트를 한꺼번에 동기화합니다.
     */
    ArtworkUpdateFullResponse updateArtworkFull(UUID exhibitionId, UUID artworkId, ArtworkUpdateFullRequest request);
}
