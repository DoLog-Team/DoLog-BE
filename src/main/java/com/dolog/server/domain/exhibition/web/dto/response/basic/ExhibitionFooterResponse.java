package com.dolog.server.domain.exhibition.web.dto.response.basic;

import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ExhibitionFooterResponse {

    private String title;
    private String department;
    private String address;

    @JsonProperty("address_detail")
    private String addressDetail;

    private String email;
    private String copyright;

    public static ExhibitionFooterResponse from(ExhibitionDetail detail) {
        return ExhibitionFooterResponse.builder()
                .title(detail.getTitle())
                .department(detail.getExhibition().getDeptName())
                .address(detail.getAddress())
                .addressDetail(detail.getAddressDetail())
                .email(detail.getEmail())
                .copyright(detail.getCopyright())
                .build();
    }
}
