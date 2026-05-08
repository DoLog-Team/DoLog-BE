package com.dolog.server.domain.exhibition.web.dto.response.basic;

import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class ExhibitionListItemResponse {
    private String id;
    private String slug;
    private String title;
    private String univName;
    private String deptName;
    private String imageUrl;
    private String startDate;
    private String endDate;
    private String address;

    private Long dDay;
    private String logoImg;

    public static ExhibitionListItemResponse of(
            Exhibition exhibition,
            ExhibitionDetail detail,
            LocalDate today
    ) {
        Long dDay = null;
        if (detail != null && detail.getStartDate() != null) {
            dDay = ChronoUnit.DAYS.between(today, detail.getStartDate());
        }

        return ExhibitionListItemResponse.builder()
                .id(exhibition.getId().toString())
                .slug(exhibition.getSlug())
                .univName(exhibition.getUnivName())
                .deptName(exhibition.getDeptName())

                // 상세 정보가 있으면 그 값을, 없으면 기본 문구나 null 반환
                .title(detail != null ? detail.getTitle() : "")
                .imageUrl(detail != null ? detail.getExhibitionImg() : null)
                .startDate(detail != null && detail.getStartDate() != null ? detail.getStartDate().toString() : null)
                .endDate(detail != null && detail.getEndDate() != null ? detail.getEndDate().toString() : null)
                .address(exhibition.getExhibitionMap() != null ? exhibition.getExhibitionMap().getAddress() : null)
                .dDay(dDay)
                .logoImg(detail != null ? detail.getLogoImg() : null)
                .build();
    }

    // 기존 fetch join 등에서 detail만 넘어올 때를 위한 호환용 메서드
    public static ExhibitionListItemResponse from(ExhibitionDetail detail, LocalDate today) {
        return of(detail.getExhibition(), detail, today);
    }
}
