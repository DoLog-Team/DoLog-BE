package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.web.dto.request.guideMap.ExhibitionGuideMapCreateRequest;
import java.util.List;
import java.util.UUID;

public interface ExhibitionGuideMapService {
    void createGuideMaps(UUID exhibitionId, List<ExhibitionGuideMapCreateRequest.GuideMapRequest> requests);
}