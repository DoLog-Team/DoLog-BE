package com.dolog.server.domain.exhibition.web.dto.request.partner;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PartnerMemberUpdateRequest {

    @JsonProperty("member_name")
    private String memberName;

    @JsonProperty("member_name_en")
    private String memberNameEn;

    @JsonProperty("member_email")
    private String memberEmail;

    @JsonProperty("member_image_url")
    private String memberImageUrl;
}
