package com.dolog.server.domain.exhibition.web.dto.response.map;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ExhibitionMapCreateResponse {

    @JsonProperty("map_id")
    private UUID mapId;

    @JsonProperty("exhibition_id")
    private UUID exhibitionId;
}
