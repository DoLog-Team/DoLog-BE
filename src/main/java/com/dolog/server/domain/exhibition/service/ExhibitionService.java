package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.web.dto.request.ExhibitionCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.ExhibitionDetailUpsertRequest;
import com.dolog.server.domain.exhibition.web.dto.request.ExhibitionMapCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.ExhibitionMapUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.ExhibitionUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.ExhibitionDetailUpsertResponse;
import com.dolog.server.domain.exhibition.web.dto.response.*;

import java.util.List;
import java.util.UUID;

public interface ExhibitionService {

    // 전시 조회
    ExhibitionMainResponse getMainExhibitions();
    List<ExhibitionListItemResponse> getExhibitions(Boolean isPublic, String univName, String search);

    // 전시 생성, 수정, 삭제
    ExhibitionCreateResponse createExhibition(ExhibitionCreateRequest request);
    ExhibitionMessageResponse updateExhibition(UUID exhibitionId, ExhibitionUpdateRequest request);
    ExhibitionMessageResponse deleteExhibition(UUID exhibitionId);

    ExhibitionDetailUpsertResponse upsertExhibitionDetail(UUID exhibitionId, ExhibitionDetailUpsertRequest request);
}