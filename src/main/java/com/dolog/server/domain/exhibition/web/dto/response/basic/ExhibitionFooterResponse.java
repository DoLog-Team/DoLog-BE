package com.dolog.server.domain.exhibition.web.dto.response.basic;

import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.dolog.server.domain.exhibition.entity.ExhibitionMap;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ExhibitionFooterResponse {

    @JsonProperty("exhibition_id")
    private UUID exhibitionId;
    private String title;
    private String department;
    private String address;

    @JsonProperty("detail_location")
    private String addressDetail;

    private String email;
    private String copyright;

    public static ExhibitionFooterResponse from(ExhibitionDetail detail, ExhibitionMap map) {
        return ExhibitionFooterResponse.builder()
                .exhibitionId(detail.getExhibition().getId())
                .title(detail.getTitle())
                .department(detail.getExhibition().getDeptName())
                .address(map.getAddress())
                .addressDetail(map.getDetailLocation())
                .email(detail.getEmail())
                .copyright(detail.getCopyright())
                .build();
    }
}
