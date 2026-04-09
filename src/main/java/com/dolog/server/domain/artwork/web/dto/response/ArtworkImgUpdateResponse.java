package com.dolog.server.domain.artwork.web.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class ArtworkImgUpdateResponse {
    @JsonProperty("image_id")
    private UUID imageId;
}
