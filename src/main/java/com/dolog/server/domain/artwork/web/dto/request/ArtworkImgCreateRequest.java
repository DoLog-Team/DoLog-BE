package com.dolog.server.domain.artwork.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArtworkImgCreateRequest {

    private String imageUrl;
    private String description;
    private Integer orderIndex;
}
