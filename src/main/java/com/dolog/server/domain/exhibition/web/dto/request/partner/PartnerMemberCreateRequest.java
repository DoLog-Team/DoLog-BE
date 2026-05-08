package com.dolog.server.domain.exhibition.web.dto.request.partner;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class PartnerMemberCreateRequest {

    private String memberName;

    private String memberNameEn;

    private MultipartFile memberImage;

    private String memberEmail;
}
