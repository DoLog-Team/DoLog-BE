package com.dolog.server.domain.artwork.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ArtworkImgUpdateRequest {
    @JsonProperty("image_url")
    private String imageUrl;

    private String description;

    @JsonProperty("order_index")
    private Integer orderIndex;
}
