package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.web.dto.request.zone.ExhibitionZoneBulkSaveRequest;
import com.dolog.server.domain.exhibition.web.dto.request.zone.ExhibitionZoneCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.zone.ExhibitionZoneUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.zone.ExhibitionZoneCreateResponse;
import com.dolog.server.domain.exhibition.web.dto.response.zone.ExhibitionZoneHiddenUpdateResponse;
import com.dolog.server.domain.exhibition.web.dto.response.zone.ExhibitionZoneListResponse;
import com.dolog.server.domain.exhibition.web.dto.response.zone.ExhibitionZoneUpdateResponse;

import java.util.UUID;

public interface ExhibitionZoneService {

    ExhibitionZoneListResponse getZones(UUID exhibitionId);

    ExhibitionZoneListResponse saveZones(UUID exhibitionId, UUID accountId, ExhibitionZoneBulkSaveRequest request);

    ExhibitionZoneCreateResponse createZone(UUID exhibitionId, UUID accountId, boolean isDologAdmin, ExhibitionZoneCreateRequest request);

    ExhibitionZoneUpdateResponse updateZone(UUID zoneId, UUID accountId, boolean isDologAdmin, ExhibitionZoneUpdateRequest request);

    void deleteZone(UUID zoneId, UUID accountId, boolean isDologAdmin);

    // 작품 그룹 숨김/재공개 (본인 전시 어드민 또는 두록 어드민)
    ExhibitionZoneHiddenUpdateResponse changeHidden(UUID zoneId, UUID accountId, boolean isDologAdmin, boolean hidden);
}
