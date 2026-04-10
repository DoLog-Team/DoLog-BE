package com.dolog.server.domain.exhibition.web.dto.request.partner;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PartnerPartUpdateRequest {

    @JsonProperty("part_name")
    private String partName;

    private Integer order;
}
