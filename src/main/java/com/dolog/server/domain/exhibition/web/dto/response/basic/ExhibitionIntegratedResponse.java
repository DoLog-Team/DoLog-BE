package com.dolog.server.domain.exhibition.web.dto.response.basic;

import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.dolog.server.domain.exhibition.entity.ExhibitionMap;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ExhibitionIntegratedResponse {

    @JsonProperty("exhibitionId")
    private UUID exhibitionId;

    @JsonProperty("univName")
    private String univName;

    @JsonProperty("deptName")
    private String deptName;

    @JsonProperty("title")
    private String title;

    @JsonProperty("exhibitionImg")
    private String exhibitionImg;

    @JsonProperty("startDate")
    private LocalDate startDate;

    @JsonProperty("endDate")
    private LocalDate endDate;

    @JsonProperty("description")
    private String description;

    @JsonProperty("location")
    private ExhibitionLocationResponse location;

    @JsonProperty("isPublic")
    private boolean isPublic;

    public static ExhibitionIntegratedResponse of(ExhibitionDetail detail, ExhibitionMap map) {
        return ExhibitionIntegratedResponse.builder()
                .exhibitionId(detail.getExhibition().getId())
                .univName(detail.getExhibition().getUnivName())
                .deptName(detail.getExhibition().getDeptName())
                .title(detail.getTitle())
                .exhibitionImg(detail.getExhibitionImg())
                .startDate(detail.getStartDate())
                .endDate(detail.getEndDate())
                .description(detail.getDescription())
                .location(ExhibitionLocationResponse.from(map))
                .isPublic(detail.getExhibition().isPublic())
                .build();
    }
}
