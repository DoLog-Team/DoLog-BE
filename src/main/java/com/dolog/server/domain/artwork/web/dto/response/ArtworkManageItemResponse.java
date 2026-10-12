package com.dolog.server.domain.artwork.web.dto.response;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import com.dolog.server.domain.artwork.entity.enums.ArtworkVisibility;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ArtworkManageItemResponse {

    private UUID artworkId;
    private String title;
    private String mainImg;
    private List<String> artistNames;
    private UUID zoneId;
    private String zoneName;
    private ArtworkStatus status;
    private ArtworkVisibility visibility;
    private boolean hidden;
    private boolean planLimitExceeded;
    private int completionRate;

    public static ArtworkManageItemResponse of(Artwork artwork, int completionRate) {
        ExhibitionZone zone = artwork.getExhibitionZone();
        return ArtworkManageItemResponse.builder()
                .artworkId(artwork.getId())
                .title(artwork.getTitle())
                .mainImg(artwork.getMainImg())
                .artistNames(artwork.getArtworkArtistMaps().stream()
                        .map(ArtworkArtistMap::getArtist)
                        .map(artist -> artist.getNameKo())
                        .toList())
                .zoneId(zone != null ? zone.getId() : null)
                .zoneName(zone != null ? zone.getName() : null)
                .status(artwork.getStatus())
                .visibility(ArtworkVisibility.of(artwork.getStatus(), completionRate))
                .hidden(artwork.isHidden())
                // 숨긴 작품은 한도 계산에서 빠지므로 남아 있는 표시는 무시한다
                .planLimitExceeded(!artwork.isHidden() && artwork.exceedsPlanLimit())
                .completionRate(completionRate)
                .build();
    }
}
