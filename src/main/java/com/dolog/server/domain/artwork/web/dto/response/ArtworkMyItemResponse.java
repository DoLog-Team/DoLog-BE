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

    // 작품 작성 완료율(%). 작품 기본/상세 필수값 + 출품 정보 필수값(출품한 경우)을 합산한다.
    private int completionRate;

    private LocalDateTime createdAt;

    public static ArtworkMyItemResponse of(Artwork artwork, String exhibitionName, long likeCount, int completionRate) {
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
