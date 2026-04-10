package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.web.dto.request.zone.ExhibitionZoneCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.zone.ExhibitionZoneUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.zone.ExhibitionZoneCreateResponse;
import com.dolog.server.domain.exhibition.web.dto.response.zone.ExhibitionZoneListResponse;
import com.dolog.server.domain.exhibition.web.dto.response.zone.ExhibitionZoneUpdateResponse;

import java.util.UUID;

public interface ExhibitionZoneService {

    ExhibitionZoneListResponse getZones(UUID exhibitionId);

    ExhibitionZoneCreateResponse createZone(UUID exhibitionId, ExhibitionZoneCreateRequest request);

    ExhibitionZoneUpdateResponse updateZone(UUID exhibitionId, UUID zoneId, ExhibitionZoneUpdateRequest request);

    void deleteZone(UUID exhibitionId, UUID zoneId);
}
