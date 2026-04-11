package com.dolog.server.domain.exhibition.web.dto.response.partner;

import com.dolog.server.domain.exhibition.entity.Partner;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class PartnerPartResponse {

    @JsonProperty("part_id")
    private UUID partId;

    public static PartnerPartResponse from(Partner partner) {
        return PartnerPartResponse.builder()
                .partId(partner.getId())
                .build();
    }
}
