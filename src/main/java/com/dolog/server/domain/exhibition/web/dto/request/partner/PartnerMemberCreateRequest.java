package com.dolog.server.domain.exhibition.web.dto.request.partner;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PartnerMemberCreateRequest {

    @NotBlank
    @JsonProperty("member_name")
    private String memberName;

    @JsonProperty("member_image_url")
    private String memberImageUrl;

    @JsonProperty("member_email")
    private String memberEmail;
}
