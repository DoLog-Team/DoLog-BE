package com.dolog.server.domain.exhibition.web.dto.request.zone;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ExhibitionZoneCreateRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 1000)
    @JsonProperty("desc")
    private String description;
    @JsonProperty("order_id")
    private Integer orderId;
}
