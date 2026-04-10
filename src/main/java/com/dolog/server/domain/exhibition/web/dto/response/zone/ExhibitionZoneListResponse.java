package com.dolog.server.domain.exhibition.web.dto.response.zone;

import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
public class ExhibitionZoneListResponse {

    private List<ZoneItem> zones;

    public static ExhibitionZoneListResponse from(List<ExhibitionZone> zones) {
        return ExhibitionZoneListResponse.builder()
                .zones(zones.stream().map(ZoneItem::from).collect(Collectors.toList()))
                .build();
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ZoneItem {

        private UUID id;
        private String name;

        @JsonProperty("desc")
        private String description;

        public static ZoneItem from(ExhibitionZone zone) {
            return ZoneItem.builder()
                    .id(zone.getId())
                    .name(zone.getName())
                    .description(zone.getDescription())
                    .build();
        }
    }
}
