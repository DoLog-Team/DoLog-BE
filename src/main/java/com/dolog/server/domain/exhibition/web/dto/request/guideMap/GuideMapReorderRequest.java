package com.dolog.server.domain.exhibition.web.dto.request.guideMap;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class GuideMapReorderRequest {

    @JsonProperty("guide_map_id")
    private UUID guideMapId;

    private Integer order;
}
