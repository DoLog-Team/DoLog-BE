package com.dolog.server.domain.exhibition.web.dto.request.partner;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class PartnerMemberReorderRequest {

    @JsonProperty("member_id")
    private UUID memberId;

    private Integer order;
}
