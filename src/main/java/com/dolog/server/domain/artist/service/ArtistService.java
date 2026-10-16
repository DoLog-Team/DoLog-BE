package com.dolog.server.domain.artist.service;

import com.dolog.server.domain.artist.web.dto.request.ArtistCreateRequest;
import com.dolog.server.domain.artist.web.dto.request.ArtistProfileUpdateRequest;
import com.dolog.server.domain.artist.web.dto.response.ArtistCreateResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistPublicResponse;
import com.dolog.server.domain.artist.web.dto.request.ArtistUpdateRequest;
import com.dolog.server.domain.artist.web.dto.response.ArtistListResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileUpdateResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistUpdateResponse;
import com.dolog.server.domain.artist.web.dto.response.MyArtistProfileResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

public interface ArtistService {

    ArtistCreateResponse createArtist(UUID accountId, ArtistCreateRequest request);

    ArtistUpdateResponse updateMyArtist(
            UUID accountId,
            ArtistUpdateRequest request
    );

    ArtistProfileUpdateResponse updateMyArtistProfile(
            UUID accountId,
            ArtistProfileUpdateRequest request,
            MultipartFile profileImg
    ) throws IOException;

    ArtistProfileUpdateResponse updateArtistProfile(
            UUID accountId,
            UUID profileId,
            ArtistProfileUpdateRequest request,
            MultipartFile profileImg
    ) throws IOException;

    MyArtistProfileResponse getMyArtistProfile(UUID accountId);

    ArtistUpdateResponse updateArtist(UUID accountId, UUID artistId, ArtistUpdateRequest request);

    void deleteArtist(UUID accountId, UUID artistId);

    ArtistListResponse getArtists(String search, int page, int size);

    ArtistPublicResponse getArtist(UUID artistId, String visitorId);
}
