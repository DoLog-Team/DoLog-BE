package com.dolog.server.domain.exhibition.web.dto.request.zone;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ExhibitionZoneUpdateRequest {

    private String name;

    @JsonProperty("desc")
    private String description;

    @JsonProperty("order_id")
    private Integer orderId;
}
