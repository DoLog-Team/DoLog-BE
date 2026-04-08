package com.dolog.server.domain.artwork.service;

import com.dolog.server.domain.artwork.web.dto.request.ArtworkCreateRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkImgCreateRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkUpdateRequest;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkCreateResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkImgCreateResponse;
import java.util.List;
import java.util.UUID;

public interface ArtworkService {
    Object getArtworks(Boolean main, String category, String search);

    ArtworkCreateResponse createArtwork(UUID exhibitionId, ArtworkCreateRequest request);

    ArtworkImgCreateResponse createArtworkImages(UUID artworkId, List<ArtworkImgCreateRequest> requests);

    ArtworkCreateResponse updateArtwork(UUID exhibitionId, UUID artworkId, ArtworkUpdateRequest request);
    void deleteArtwork(UUID exhibitionId, UUID artworkId);
}
