package com.dolog.server.domain.artwork.web.dto.response;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ExhibitionFieldSettings;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

// 작가 본인의 "작품 출품 상태" 조회 (GET /artworks/{artworkId}/exhibition)
@Getter
@Builder
public class ArtworkExhibitionStatusResponse {

    private UUID exhibitionId;
    private String exhibitionName;
    private UUID zoneId;
    private String zoneName;
    private boolean hidden;
    private boolean planLimitExceeded;
    // 전시의 항목 필수/숨김 설정. 작품 자체의 hidden(숨김 여부)과는 다른 개념이라 fieldSettings로 따로 묶는다.
    private FieldSettingsResponse fieldSettings;

    public static ArtworkExhibitionStatusResponse of(
            Artwork artwork, String exhibitionName, ExhibitionFieldSettings fieldSettings
    ) {
        return ArtworkExhibitionStatusResponse.builder()
                .exhibitionId(artwork.getExhibition().getId())
                .exhibitionName(exhibitionName)
                .zoneId(artwork.getExhibitionZone() != null ? artwork.getExhibitionZone().getId() : null)
                .zoneName(artwork.getExhibitionZone() != null ? artwork.getExhibitionZone().getName() : null)
                .hidden(artwork.isHidden())
                .planLimitExceeded(artwork.exceedsPlanLimit())
                .fieldSettings(FieldSettingsResponse.from(fieldSettings))
                .build();
    }
}
