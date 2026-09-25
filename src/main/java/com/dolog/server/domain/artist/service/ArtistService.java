package com.dolog.server.domain.artist.service;

import com.dolog.server.domain.artist.web.dto.request.ArtistCreateRequest;
import com.dolog.server.domain.artist.web.dto.response.ArtistCreateResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistResponse;
import com.dolog.server.domain.artist.web.dto.request.ArtistUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface ArtistService {

    ArtistCreateResponse createArtist(UUID accountId, ArtistCreateRequest request);

    ArtistResponse updateArtist(UUID artistId, ArtistUpdateRequest request);

    ArtistResponse deleteArtist(UUID artistId);

    List<ArtistResponse> getArtists();

    ArtistResponse getArtist(UUID artistId);
}