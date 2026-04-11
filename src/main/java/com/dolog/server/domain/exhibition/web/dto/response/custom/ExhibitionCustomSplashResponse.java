package com.dolog.server.domain.exhibition.web.dto.response.custom;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ExhibitionCustomSplashResponse {

    @JsonProperty("exhibition_id")
    private UUID exhibitionId;

    @JsonProperty("splash_img")
    private String splashImg;

    public static ExhibitionCustomSplashResponse of(UUID exhibitionId, String splashImg) {
        return ExhibitionCustomSplashResponse.builder()
                .exhibitionId(exhibitionId)
                .splashImg(splashImg)
                .build();
    }
}
