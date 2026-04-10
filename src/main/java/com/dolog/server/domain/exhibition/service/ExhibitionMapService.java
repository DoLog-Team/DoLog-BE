package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.web.dto.request.map.ExhibitionMapCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.map.ExhibitionMapUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.map.ExhibitionMapCreateResponse;
import com.dolog.server.domain.exhibition.web.dto.response.map.ExhibitionMapUpdateResponse;

import java.util.UUID;

public interface ExhibitionMapService {
    // 전시 지도 추가, 수정, 삭제
    ExhibitionMapCreateResponse createExhibitionMap(UUID exhibitionId, ExhibitionMapCreateRequest request);
    ExhibitionMapUpdateResponse updateExhibitionMap(UUID exhibitionId, ExhibitionMapUpdateRequest request);
    void deleteExhibitionMap(UUID exhibitionId);
}
