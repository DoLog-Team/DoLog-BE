package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.web.dto.request.guideMap.ExhibitionGuideMapCreateRequest;
import java.util.UUID;

public interface ExhibitionGuideMapService {
    void createGuideMap(
            UUID exhibitionId,
            ExhibitionGuideMapCreateRequest request
    );

    void updateGuideMap(UUID guideMapId, ExhibitionGuideMapCreateRequest request);

    void deleteGuideMap(UUID guideMapId);
}