package com.dolog.server.domain.exhibition.web.dto.response.partner;

import com.dolog.server.domain.exhibition.entity.PartnerMember;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class PartnerMemberUpdateResponse {

    @JsonProperty("member_id")
    private UUID memberId;

    public static PartnerMemberUpdateResponse from(PartnerMember member) {
        return PartnerMemberUpdateResponse.builder()
                .memberId(member.getId())
                .build();
    }
}
