package com.dolog.server.domain.exhibition.web.dto.response.map;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExhibitionMapUpdateResponse {

    @JsonProperty("map_id")
    private String mapId;
}
