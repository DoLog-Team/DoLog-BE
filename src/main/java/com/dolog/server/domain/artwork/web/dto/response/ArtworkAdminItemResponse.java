package com.dolog.server.domain.artwork.web.dto.response;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// 두록 어드민 전용 작품 전체 조회 항목. 공개 여부와 무관하게 다 보여준다.
@Getter
@Builder
public class ArtworkAdminItemResponse {

    private UUID artworkId;
    private String title;
    private UUID exhibitionId;
    private String exhibitionName;
    private String category;
    private ArtworkStatus status;
    private boolean hidden;
    private boolean planLimitExceeded;
    private List<String> artistNames;
    private String mainImg;
    private LocalDateTime createdAt;

    public static ArtworkAdminItemResponse from(Artwork artwork, String exhibitionName) {
        return ArtworkAdminItemResponse.builder()
                .artworkId(artwork.getId())
                .title(artwork.getTitle())
                .exhibitionId(artwork.getExhibition() != null ? artwork.getExhibition().getId() : null)
                .exhibitionName(exhibitionName)
                .category(artwork.getCategory())
                .status(artwork.getStatus())
                .hidden(artwork.isHidden())
                .planLimitExceeded(artwork.exceedsPlanLimit())
                .artistNames(artwork.getArtworkArtistMaps().stream()
                        .map(ArtworkArtistMap::getArtist)
                        .map(artist -> artist.getNameKo())
                        .toList())
                .mainImg(artwork.getMainImg())
                .createdAt(artwork.getCreatedAt())
                .build();
    }
}
