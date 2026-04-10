package com.dolog.server.domain.exhibition.web.dto.response.zone;

import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ExhibitionZoneUpdateResponse {

    private UUID id;

    public static ExhibitionZoneUpdateResponse from(ExhibitionZone zone) {
        return ExhibitionZoneUpdateResponse.builder()
                .id(zone.getId())
                .build();
    }
}
