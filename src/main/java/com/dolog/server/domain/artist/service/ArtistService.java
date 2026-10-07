package com.dolog.server.domain.artist.service;

import com.dolog.server.domain.artist.web.dto.request.ArtistCreateRequest;
import com.dolog.server.domain.artist.web.dto.response.ArtistCreateResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistPublicResponse;
import com.dolog.server.domain.artist.web.dto.request.ArtistUpdateRequest;
import com.dolog.server.domain.artist.web.dto.response.ArtistListResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistUpdateResponse;

import java.util.UUID;

public interface ArtistService {

    ArtistCreateResponse createArtist(UUID accountId, ArtistCreateRequest request);

    ArtistUpdateResponse updateArtist(UUID accountId, UUID artistId, ArtistUpdateRequest request);

    void deleteArtist(UUID accountId, UUID artistId);

    ArtistListResponse getArtists(String search, int page, int size);

    ArtistPublicResponse getArtist(UUID artistId, String visitorId);
}
