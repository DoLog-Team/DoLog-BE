package com.dolog.server.domain.exhibition.web.dto.response.basic;

import com.dolog.server.domain.exhibition.entity.ExhibitionMap;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ExhibitionLocationResponse {

    private String address;

    @JsonProperty("detail_location")
    private String detailLocation;

    private String latitude;
    private String longitude;

    public static ExhibitionLocationResponse from(ExhibitionMap map) {
        return ExhibitionLocationResponse.builder()
                .address(map.getAddress())
                .detailLocation(map.getDetailLocation())
                .latitude(map.getLatitude())
                .longitude(map.getLongitude())
                .build();
    }
}
