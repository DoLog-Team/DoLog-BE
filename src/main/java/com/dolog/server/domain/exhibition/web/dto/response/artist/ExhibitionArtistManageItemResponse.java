package com.dolog.server.domain.exhibition.web.dto.response.artist;

import java.util.UUID;

public record ExhibitionArtistManageItemResponse(
        UUID artistId,
        String nameKo,
        String email,
        String greeting,
        Integer artworkCount
) {
    // JPQL COUNT 결과(Long)를 API 명세의 int 타입(Integer)으로 변환한다.
    public ExhibitionArtistManageItemResponse(
            UUID artistId,
            String nameKo,
            String email,
            String greeting,
            Long artworkCount
    ) {
        this(
                artistId,
                nameKo,
                email,
                greeting,
                artworkCount == null ? null : Math.toIntExact(artworkCount)
        );
    }
}
