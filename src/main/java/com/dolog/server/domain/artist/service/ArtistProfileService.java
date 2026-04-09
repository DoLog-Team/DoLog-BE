package com.dolog.server.domain.artist.service;

import com.dolog.server.domain.artist.web.dto.request.ArtistProfileCreateRequest;
import com.dolog.server.domain.artist.web.dto.request.ArtistSnsRequest;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistSnsResponse;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface ArtistProfileService {
    ArtistProfileResponse createArtistProfile(String exhibitionId, ArtistProfileCreateRequest request)
            throws Exception;

    ArtistProfileResponse updateArtistProfile(String profileId, ArtistProfileCreateRequest request)
            throws IOException;

    ArtistSnsResponse addArtistSns(String profileIdStr, ArtistSnsRequest request)
            throws IOException;

    List<ArtistSnsResponse> deleteArtistSns(UUID snsId)
            throws IOException;;

    List<ArtistSnsResponse> getArtistSnsList(String profileIdStr);

}