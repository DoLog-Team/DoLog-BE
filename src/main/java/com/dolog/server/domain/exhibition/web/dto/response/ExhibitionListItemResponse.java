package com.dolog.server.domain.exhibition.web.dto.response;

import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ExhibitionListItemResponse {
    private String id;
    private String title;
    private String univName;
    private String deptName;
    private String imageUrl;
    private String startDate;
    private String endDate;

    public static ExhibitionListItemResponse of(Exhibition exhibition, ExhibitionDetail detail) {
        return ExhibitionListItemResponse.builder()
                .id(exhibition.getId().toString())
                .univName(exhibition.getUnivName())
                .deptName(exhibition.getDeptName())
                // 상세 정보가 있으면 그 값을, 없으면 기본 문구나 null 반환
                .title(detail != null ? detail.getTitle() : "")
                .imageUrl(detail != null ? detail.getExhibitionImg() : null)
                .startDate(detail != null && detail.getStartDate() != null ? detail.getStartDate().toString() : null)
                .endDate(detail != null && detail.getEndDate() != null ? detail.getEndDate().toString() : null)
                .build();
    }

    // 기존 fetch join 등에서 detail만 넘어올 때를 위한 호환용 메서드
    public static ExhibitionListItemResponse from(ExhibitionDetail detail) {
        return of(detail.getExhibition(), detail);
    }
}
