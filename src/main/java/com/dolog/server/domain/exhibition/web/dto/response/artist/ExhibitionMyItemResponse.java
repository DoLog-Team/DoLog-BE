package com.dolog.server.domain.exhibition.web.dto.response.artist;

import com.dolog.server.domain.exhibition.entity.ExhibitionArtistMap;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

// 마이페이지 "내 전시 목록" 항목
@Getter
@Builder
public class ExhibitionMyItemResponse {

    private UUID exhibitionId;
    private String exhibitionName;
    private ExhibitionArtistStatus status;
    private LocalDateTime joinedAt;

    public static ExhibitionMyItemResponse from(ExhibitionArtistMap map, String exhibitionName) {
        return ExhibitionMyItemResponse.builder()
                .exhibitionId(map.getExhibition().getId())
                .exhibitionName(exhibitionName)
                .status(map.getStatus())
                .joinedAt(map.getCreatedAt())
                .build();
    }
}
