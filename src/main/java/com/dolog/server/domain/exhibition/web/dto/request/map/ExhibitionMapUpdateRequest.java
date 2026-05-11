package com.dolog.server.domain.exhibition.web.dto.request.map;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ExhibitionMapUpdateRequest {

    private String address;

    @JsonProperty("detail_location")
    private String detailLocation;

    private String latitude;

    private String longitude;

    @JsonProperty("location_description")
    private String locationDescription;
}
