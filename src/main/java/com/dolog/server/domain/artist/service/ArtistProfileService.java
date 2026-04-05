package com.dolog.server.domain.artist.service;

import com.dolog.server.domain.artist.web.dto.request.ArtistProfileCreateRequest;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileResponse;

public interface ArtistProfileService {
    ArtistProfileResponse createArtistProfile(String exhibitionId, ArtistProfileCreateRequest request)
            throws Exception;
}