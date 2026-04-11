package com.dolog.server.domain.exhibition.web.dto.request.custom;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ExhibitionCustomSplashRequest {

    @JsonProperty("splash_img")
    private String splashImg;
}
