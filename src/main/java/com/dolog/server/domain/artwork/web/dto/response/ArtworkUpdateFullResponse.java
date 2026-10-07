package com.dolog.server.domain.artwork.web.dto.response;

import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.entity.ArtworkImg;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ArtworkUpdateFullResponse {
    private UUID artworkId;
    private String title;
    private List<UUID> updatedArtistIds; // 연결된 작가의 전시별 프로필 ID
    private List<UUID> updatedImageIds;
    private ArtworkStatus status;
    private boolean hidden;

    public static ArtworkUpdateFullResponse from(Artwork artwork) {
        return ArtworkUpdateFullResponse.builder()
                .artworkId(artwork.getId())
                .title(artwork.getTitle())
                .updatedArtistIds(artwork.getArtworkArtistMaps().stream()
                        .map(ArtworkArtistMap::getArtistProfile)
                        .filter(profile -> profile != null)
                        .map(profile -> profile.getId())
                        .toList())
                .updatedImageIds(artwork.getArtworkImg().stream().map(ArtworkImg::getId).toList())
                .status(artwork.getStatus())
                .hidden(artwork.isHidden())
                .build();
    }
}
