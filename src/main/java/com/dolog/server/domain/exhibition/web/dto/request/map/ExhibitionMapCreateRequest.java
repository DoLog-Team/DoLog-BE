package com.dolog.server.domain.exhibition.web.dto.request.map;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ExhibitionMapCreateRequest {

    @NotBlank(message = "필수 값이 누락되었습니다.")
    private String address;

    @JsonProperty("detail_location")
    private String detailLocation;

    @NotBlank(message = "필수 값이 누락되었습니다.")
    private String latitude;

    @NotBlank(message = "필수 값이 누락되었습니다.")
    private String longitude;

    @JsonProperty("location_description")
    private String locationDescription;
}
