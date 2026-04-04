package com.dolog.server.domain.artist.service;

import com.dolog.server.domain.artist.web.dto.ArtistCreateRequest;
import com.dolog.server.domain.artist.web.dto.ArtistResponse;
import com.dolog.server.domain.artist.web.dto.ArtistUpdateRequest;

import java.util.UUID;

public interface ArtistService {

    ArtistResponse createArtist(ArtistCreateRequest request);

    ArtistResponse updateArtist(UUID artistId, ArtistUpdateRequest request);

    ArtistResponse deleteArtist(UUID artistId);
}