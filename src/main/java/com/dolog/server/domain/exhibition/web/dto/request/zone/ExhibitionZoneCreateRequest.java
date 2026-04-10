package com.dolog.server.domain.exhibition.web.dto.request.zone;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ExhibitionZoneCreateRequest {

    @NotBlank
    private String name;

    @JsonProperty("desc")
    private String description;
}
