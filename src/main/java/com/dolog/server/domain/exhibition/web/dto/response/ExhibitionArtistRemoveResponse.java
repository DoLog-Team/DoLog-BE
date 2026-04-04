package com.dolog.server.domain.exhibition.web.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ExhibitionArtistRemoveResponse {

    private UUID exhibitionId;
    private String exhibitionName;

    private UUID artistId;
    private String artistName;

    private String message;

    public static ExhibitionArtistRemoveResponse of(
            UUID exhibitionId,
            String exhibitionName,
            UUID artistId,
            String artistName
    ) {
        String message = artistName + " 님이 " + exhibitionName + "에서 삭제되었습니다.";

        return ExhibitionArtistRemoveResponse.builder()
                .exhibitionId(exhibitionId)
                .exhibitionName(exhibitionName)
                .artistId(artistId)
                .artistName(artistName)
                .message(message)
                .build();
    }
}