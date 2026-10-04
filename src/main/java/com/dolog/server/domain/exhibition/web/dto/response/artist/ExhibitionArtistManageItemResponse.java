package com.dolog.server.domain.exhibition.web.dto.response.artist;

import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ExhibitionArtistManageItemResponse(
        UUID exhibitionArtistId,
        UUID artistId,
        UUID profileId,
        String nameKo,
        String nameEn,
        String profileImg,
        ExhibitionArtistStatus status,
        String greeting,
        LocalDateTime appliedAt
) {
}
