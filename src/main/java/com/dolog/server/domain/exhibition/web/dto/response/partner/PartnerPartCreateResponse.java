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
public class PartnerPartCreateResponse {

    @JsonProperty("part_id")
    private UUID partId;

    public static PartnerPartCreateResponse from(Partner partner) {
        return PartnerPartCreateResponse.builder()
                .partId(partner.getId())
                .build();
    }
}
