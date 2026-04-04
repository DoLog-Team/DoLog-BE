package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.web.dto.request.ExhibitionCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.ExhibitionUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.*;

import java.util.List;
import java.util.UUID;

public interface ExhibitionService {

    ExhibitionMainResponse getMainExhibitions();

    List<ExhibitionListItemResponse> getExhibitions(String univName, String search);

    ExhibitionCreateResponse createExhibition(ExhibitionCreateRequest request);

    ExhibitionMessageResponse updateExhibition(UUID exhibitionId, ExhibitionUpdateRequest request);

    ExhibitionMessageResponse deleteExhibition(UUID exhibitionId);

    ExhibitionArtistAddResponse addArtistToExhibition(UUID exhibitionId, UUID artistId);

    List<ExhibitionArtistListResponse> getArtistsByExhibition(UUID exhibitionId);
}