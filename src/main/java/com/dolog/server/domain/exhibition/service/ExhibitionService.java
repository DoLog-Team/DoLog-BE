package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.entity.enums.ExhibitionType;
import com.dolog.server.domain.exhibition.web.dto.request.basic.ExhibitionCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.basic.ExhibitionDetailUpsertRequest;
import com.dolog.server.domain.exhibition.web.dto.request.basic.ExhibitionMetaUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.basic.ExhibitionUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.basic.*;
import com.dolog.server.domain.exhibition.web.dto.response.custom.ExhibitionCustomThemeResponse;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

public interface ExhibitionService {

    // 전시 조회
    ExhibitionMainResponse getMainExhibitions(String sort);
    ExhibitionResolveResponse resolveSlug(String slug);
    List<ExhibitionListItemResponse> getExhibitions(Boolean isPublic, String univName, ExhibitionType exhibitionType, String search);
    ExhibitionIntegratedResponse getExhibitionDetails(UUID exhibitionId);
    ExhibitionFooterResponse getFooterInfo(UUID exhibitionId);
    ExhibitionCustomThemeResponse getCustomTheme(UUID exhibitionId);
    ExhibitionMetaResponse getExhibitionMeta(UUID exhibitionId);

    // 전시 생성, 수정, 삭제
    ExhibitionCreateResponse createExhibition(ExhibitionCreateRequest request);
    EntryCodeResponse reissueEntryCode(UUID exhibitionId, LocalDateTime expiresAt);
    ExhibitionMessageResponse updateExhibition(UUID exhibitionId, ExhibitionUpdateRequest request);
    ExhibitionMessageResponse deleteExhibition(UUID exhibitionId);

    ExhibitionDetailUpsertResponse upsertExhibitionDetail(UUID exhibitionId, ExhibitionDetailUpsertRequest request) throws IOException;

    // 전시 Meta 정보 관련
    ExhibitionMetaResponse updateExhibitionMeta(UUID exhibitionId, ExhibitionMetaUpdateRequest request) throws IOException;
}
