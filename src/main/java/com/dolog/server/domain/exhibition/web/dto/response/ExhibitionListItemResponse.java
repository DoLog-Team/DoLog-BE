package com.dolog.server.domain.exhibition.web.dto.response;

import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExhibitionListItemResponse {

    private String id;
    private String title;
    private String univName;
    private String deptName;
    private String imageUrl;
    private String startDate;
    private String endDate;

    public static ExhibitionListItemResponse from(ExhibitionDetail detail) {
        Exhibition exhibition = detail.getExhibition();
        return ExhibitionListItemResponse.builder()
                .id(exhibition.getId().toString())
                .title(detail.getTitle())
                .univName(exhibition.getUnivName())
                .deptName(exhibition.getDeptName())
                .imageUrl(detail.getExhibitionImg())
                .startDate(detail.getStartDate() != null ? detail.getStartDate().toString() : null)
                .endDate(detail.getEndDate() != null ? detail.getEndDate().toString() : null)
                .build();
    }
}
