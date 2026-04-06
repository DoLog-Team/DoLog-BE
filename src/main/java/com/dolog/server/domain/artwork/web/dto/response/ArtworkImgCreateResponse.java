package com.dolog.server.domain.artwork.web.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ArtworkImgCreateResponse {

    @JsonProperty("artwork_id")
    private UUID artworkId;

    @JsonProperty("image_ids")
    private List<UUID> imageIds;

    public static ArtworkImgCreateResponse from(UUID artworkId, List<UUID> imageIds) {
        return ArtworkImgCreateResponse.builder()
                .artworkId(artworkId)
                .imageIds(imageIds)
                .build();
    }
}