package com.dolog.server.domain.artist.service;

import com.dolog.server.domain.artist.web.dto.request.ArtistCreateRequest;
import com.dolog.server.domain.artist.web.dto.response.ArtistCreateResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistResponse;
import com.dolog.server.domain.artist.web.dto.request.ArtistUpdateRequest;
import com.dolog.server.domain.artist.web.dto.response.ArtistListResponse;

import java.util.UUID;

public interface ArtistService {

    ArtistCreateResponse createArtist(UUID accountId, ArtistCreateRequest request);

    ArtistResponse updateArtist(UUID accountId, UUID artistId, ArtistUpdateRequest request);

    void deleteArtist(UUID accountId, UUID artistId);

    ArtistListResponse getArtists(String search, int page, int size);

    ArtistResponse getArtist(UUID artistId);
}