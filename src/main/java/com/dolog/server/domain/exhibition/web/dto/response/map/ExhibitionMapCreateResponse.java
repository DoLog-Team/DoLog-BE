package com.dolog.server.domain.exhibition.web.dto.response.map;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExhibitionMapCreateResponse {

    @JsonProperty("map_id")
    private String mapId;

    @JsonProperty("exhibition_id")
    private String exhibitionId;
}
