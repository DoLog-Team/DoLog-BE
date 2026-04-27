package com.dolog.server.domain.artwork.service;

import com.dolog.server.domain.artwork.web.dto.request.ArtworkImgCreateRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkImgUpdateRequest;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkImgCreateResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkImgUpdateResponse;

import java.util.List;
import java.util.UUID;

public interface ArtworkImageService {
    ArtworkImgCreateResponse createArtworkImages(UUID artworkId, List<ArtworkImgCreateRequest> requests);
    ArtworkImgUpdateResponse updateArtworkImage(UUID artworkId, UUID imageId, ArtworkImgUpdateRequest request);
    void deleteArtworkImage(UUID artworkId, UUID imageId);
}