package com.dolog.server.domain.artist.web.dto.response;

import com.dolog.server.domain.artist.repository.projection.ArtistProfileListItemProjection;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ArtistProfileListItemResponse {

    private UUID profileId;
    private UUID artistId;
    private String nameKo;
    private String nameEn;
    private String profileImg;
    private Boolean isPublic;
    private long viewCount;
    private int likeCount;

    public static ArtistProfileListItemResponse from(
            ArtistProfileListItemProjection projection
    ) {
        return ArtistProfileListItemResponse.builder()
                .profileId(UUID.fromString(projection.getProfileId()))
                .artistId(UUID.fromString(projection.getArtistId()))
                .nameKo(projection.getNameKo())
                .nameEn(projection.getNameEn())
                .profileImg(projection.getProfileImg())
                .isPublic(Boolean.TRUE.equals(projection.getIsPublic()))
                .viewCount(projection.getViewCount())
                .likeCount(Math.toIntExact(projection.getLikeCount()))
                .build();
    }
}
