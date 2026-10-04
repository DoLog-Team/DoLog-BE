package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.web.dto.request.zone.ExhibitionZoneCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.zone.ExhibitionZoneUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.zone.ExhibitionZoneCreateResponse;
import com.dolog.server.domain.exhibition.web.dto.response.zone.ExhibitionZoneHiddenUpdateResponse;
import com.dolog.server.domain.exhibition.web.dto.response.zone.ExhibitionZoneListResponse;
import com.dolog.server.domain.exhibition.web.dto.response.zone.ExhibitionZoneUpdateResponse;

import java.util.UUID;

public interface ExhibitionZoneService {

    ExhibitionZoneListResponse getZones(UUID exhibitionId);

    ExhibitionZoneCreateResponse createZone(UUID exhibitionId, ExhibitionZoneCreateRequest request);

    ExhibitionZoneUpdateResponse updateZone(UUID zoneId, ExhibitionZoneUpdateRequest request);

    void deleteZone(UUID zoneId);

    // 작품 그룹 숨김/재공개 (본인 전시 어드민 또는 두록 어드민)
    ExhibitionZoneHiddenUpdateResponse changeHidden(UUID zoneId, UUID accountId, boolean isDologAdmin, boolean hidden);
}
