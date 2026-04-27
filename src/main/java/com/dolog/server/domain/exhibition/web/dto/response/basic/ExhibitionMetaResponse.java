package com.dolog.server.domain.exhibition.web.dto.response.basic;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
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

    public static ExhibitionMetaResponse of(Exhibition exhibition, ExhibitionDetail detail) {
        return ExhibitionMetaResponse.builder()
                .id(exhibition.getSchoolId())
                .title(detail.getTitle())
                .description(detail.getDescription())
                .image(detail.getExhibitionImg())
                .build();
    }
}
