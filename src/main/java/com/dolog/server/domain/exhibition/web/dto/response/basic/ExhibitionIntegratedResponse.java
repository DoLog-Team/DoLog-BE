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

    private UUID exhibitionId;
    private String univName;
    private String deptName;
    private String title;
    private String exhibitionImg;
    private LocalDate startDate;
    private LocalDate endDate;
    private String dateInfo;
    private String description;
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
                .dateInfo(detail.getDateInfo())
                .description(detail.getDescription())
                .location(map != null ? ExhibitionLocationResponse.from(map) : null)
                .isPublic(detail.getExhibition().isPublic())
                .build();
    }
}
