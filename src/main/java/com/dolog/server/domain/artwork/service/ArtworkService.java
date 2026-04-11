package com.dolog.server.domain.artwork.service;

import com.dolog.server.domain.artwork.web.dto.request.*;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkArtistMappingResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkCreateResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkImgCreateResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkImgUpdateResponse;

import java.util.List;
import java.util.UUID;

public interface ArtworkService {
    Object getArtworks(Boolean main, String category, String search);

    ArtworkCreateResponse createArtwork(UUID exhibitionId, ArtworkCreateRequest request);

    ArtworkImgCreateResponse createArtworkImages(UUID artworkId, List<ArtworkImgCreateRequest> requests);

    ArtworkCreateResponse updateArtwork(UUID exhibitionId, UUID artworkId, ArtworkUpdateRequest request);

    void deleteArtwork(UUID exhibitionId, UUID artworkId);

    ArtworkImgUpdateResponse updateArtworkImage(UUID artworkId, UUID imageId, ArtworkImgUpdateRequest request);

    void deleteArtworkImage(UUID artworkId, UUID imageId);

    ArtworkArtistMappingResponse createArtistMapping(UUID artworkId, ArtworkArtistMappingRequest request);

    ArtworkArtistMappingResponse updateArtistMapping(UUID artworkId, UUID artistId, ArtworkArtistMappingRequest request);

    void deleteArtistMapping(UUID artworkId, UUID artistId);
}
