package com.dolog.server.domain.exhibition.web.dto.response.basic;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ExhibitionMetaResponse {

    private String id;
    private String title;
    private String description;
    private String image;
    private String favicon;

    private String schoolName;     // 학교명
    private String collegeName;    // 단과대명
    private String departmentName; // 학과/전공명
    private ExhibitionType exhibitionType; // 전시 유형 (ONLINE, OFFLINE 등)
    private String url;            // slug 기반의 Full URL

    public static ExhibitionMetaResponse of(Exhibition exhibition, ExhibitionDetail detail) {

        String fullUrl = "https://dolog.kr/exhibitions/" + exhibition.getSlug();

        return ExhibitionMetaResponse.builder()
                .id(exhibition.getId().toString())
                .title(detail.getOgTitle() != null ? detail.getOgTitle() : detail.getTitle())
                .description(detail.getOgDescription() != null ? detail.getOgDescription() : detail.getDescription())
                .image(detail.getOgImage() != null ? detail.getOgImage() : detail.getExhibitionImg())
                .favicon(detail.getFaviconImg())

                // 엔티티로부터 추가 정보 매핑
                .schoolName(exhibition.getUnivName())
                // 기존 데이터가 null 이면 안전하게 빈 문자열("")로 치환하여 반환
                .collegeName(exhibition.getCollegeName() != null ? exhibition.getCollegeName() : "")
                .departmentName(exhibition.getDeptName())
                .exhibitionType(exhibition.getExhibitionType())
                .url(fullUrl)
                .build();
    }
}
