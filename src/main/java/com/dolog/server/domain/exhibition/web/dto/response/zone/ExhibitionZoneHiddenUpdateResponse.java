package com.dolog.server.domain.exhibition.web.dto.response.zone;

import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ExhibitionZoneHiddenUpdateResponse {

    private UUID zoneId;
    private boolean hidden;

    public static ExhibitionZoneHiddenUpdateResponse from(ExhibitionZone zone) {
        return ExhibitionZoneHiddenUpdateResponse.builder()
                .zoneId(zone.getId())
                .hidden(zone.isHidden())
                .build();
    }
}
