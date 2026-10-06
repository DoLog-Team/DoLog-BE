package com.dolog.server.domain.exhibition.web.dto.request.artist;

import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;

public enum ExhibitionArtistManageStatus {
    PENDING(ExhibitionArtistStatus.PENDING),
    JOINED(ExhibitionArtistStatus.JOINED);

    private final ExhibitionArtistStatus exhibitionArtistStatus;

    ExhibitionArtistManageStatus(
            ExhibitionArtistStatus exhibitionArtistStatus
    ) {
        this.exhibitionArtistStatus = exhibitionArtistStatus;
    }

    public ExhibitionArtistStatus toExhibitionArtistStatus() {
        return exhibitionArtistStatus;
    }
}
