package com.dolog.server.domain.bts.web.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class BtsMappingUpdateResponse {

    @JsonProperty("bts_id")
    private UUID btsId;

    private String title;

    @JsonProperty("artist_profile_id")
    private UUID artistProfileId;

    @JsonProperty("updated_artwork_ids")
    private List<UUID> updatedArtworkIds;
}
