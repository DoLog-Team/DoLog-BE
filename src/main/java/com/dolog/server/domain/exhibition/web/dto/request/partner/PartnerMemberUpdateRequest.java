package com.dolog.server.domain.exhibition.web.dto.request.partner;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class PartnerMemberUpdateRequest {

    private String memberName;

    private String memberNameEn;

    private String memberEmail;

    private MultipartFile memberImage;
}
