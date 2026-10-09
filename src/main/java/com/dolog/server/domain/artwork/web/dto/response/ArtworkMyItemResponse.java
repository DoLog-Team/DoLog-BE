package com.dolog.server.domain.artwork.web.dto.response;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

// 마이페이지 "내 작품 목록" 항목
@Getter
@Builder
public class ArtworkMyItemResponse {

    private UUID artworkId;
    private String title;
    private String mainImg;
    private ArtworkStatus status;
    private boolean hidden;
    private UUID exhibitionId;
    private String exhibitionName;
    private long viewCount;
    private long likeCount;

    // 전시 필수 항목 기준 작성률(%). 출품 전 개인 작품은 기준이 없어 null.
    private Integer completionRate;

    private LocalDateTime createdAt;

    public static ArtworkMyItemResponse of(Artwork artwork, String exhibitionName, long likeCount, Integer completionRate) {
        return ArtworkMyItemResponse.builder()
                .artworkId(artwork.getId())
                .title(artwork.getTitle())
                .mainImg(artwork.getMainImg())
                .status(artwork.getStatus())
                .hidden(artwork.isHidden())
                .exhibitionId(artwork.getExhibition() != null ? artwork.getExhibition().getId() : null)
                .exhibitionName(exhibitionName)
                .viewCount(artwork.getViewCount())
                .likeCount(likeCount)
                .completionRate(completionRate)
                .createdAt(artwork.getCreatedAt())
                .build();
    }
}
