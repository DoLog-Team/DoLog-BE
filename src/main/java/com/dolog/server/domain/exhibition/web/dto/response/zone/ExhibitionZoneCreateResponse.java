package com.dolog.server.domain.exhibition.web.dto.response.zone;

import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ExhibitionZoneCreateResponse {

    private UUID id;

    public static ExhibitionZoneCreateResponse from(ExhibitionZone zone) {
        return ExhibitionZoneCreateResponse.builder()
                .id(zone.getId())
                .build();
    }
}
