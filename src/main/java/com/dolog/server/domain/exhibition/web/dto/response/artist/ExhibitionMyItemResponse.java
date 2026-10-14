package com.dolog.server.domain.exhibition.web.dto.response.artist;

import com.dolog.server.domain.exhibition.entity.ExhibitionArtistMap;
import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

// 마이페이지 "내 전시 목록" 항목
@Getter
@Builder
public class ExhibitionMyItemResponse {

    private UUID exhibitionId;
    private String exhibitionName;
    private String exhibitionImg;
    private LocalDate startDate;
    private LocalDate endDate;
    private ExhibitionArtistStatus status;
    private LocalDateTime joinedAt;

    public static ExhibitionMyItemResponse from(ExhibitionArtistMap map, String exhibitionName) {
        ExhibitionDetail detail = map.getExhibition().getExhibitionDetail();
        return ExhibitionMyItemResponse.builder()
                .exhibitionId(map.getExhibition().getId())
                .exhibitionName(exhibitionName)
                .exhibitionImg(detail != null ? detail.getExhibitionImg() : null)
                .startDate(detail != null ? detail.getStartDate() : null)
                .endDate(detail != null ? detail.getEndDate() : null)
                .status(map.getStatus())
                .joinedAt(map.getCreatedAt())
                .build();
    }
}
