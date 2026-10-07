package com.dolog.server.domain.artist.service;

import com.dolog.server.domain.artist.web.dto.request.ArtistProfileCreateRequest;
import com.dolog.server.domain.artist.web.dto.request.ArtistSnsRequest;
import com.dolog.server.domain.artist.web.dto.request.ArtistSnsUpdateRequest;
import com.dolog.server.domain.artist.web.dto.request.ArtistProfileUpdateRequest;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileCreateResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileDetailResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileListResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistSnsCreateResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistSnsListResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistSnsUpdateResponse;
import com.dolog.server.domain.artist.web.dto.response.ArtistProfileUpdateResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

public interface ArtistProfileService {

//    프로필
    ArtistProfileCreateResponse createArtistProfile(
            ArtistProfileCreateRequest request,
            MultipartFile profileImg
    ) throws IOException;

    ArtistProfileUpdateResponse updateArtistProfile(
            UUID accountId,
            UUID profileId,
            ArtistProfileUpdateRequest request,
            MultipartFile profileImg
    )
            throws IOException;

    ArtistProfileListResponse getArtistProfileList(
            UUID accountId,
            UUID exhibitionId
    );

    ArtistProfileDetailResponse getArtistProfileDetail(
            UUID accountId,
            UUID profileId,
            String visitorId
    );

//    SNS
    ArtistSnsCreateResponse addArtistSns(
            UUID accountId,
            UUID profileId,
            ArtistSnsRequest request
    );

    void deleteArtistSns(UUID accountId, UUID snsId);

    ArtistSnsListResponse getArtistSnsList(
            UUID accountId,
            UUID profileId
    );

    ArtistSnsUpdateResponse updateArtistSns(
            UUID accountId,
            UUID snsId,
            ArtistSnsUpdateRequest request
    );

}
