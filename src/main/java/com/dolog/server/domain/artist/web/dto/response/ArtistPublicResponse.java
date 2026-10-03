package com.dolog.server.domain.artist.web.dto.response;

import com.dolog.server.domain.artist.entity.Artist;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ArtistPublicResponse {

    private UUID id;
    private String nameKo;
    private String nameEn;

    public static ArtistPublicResponse from(Artist artist) {
        return ArtistPublicResponse.builder()
                .id(artist.getId())
                .nameKo(artist.getNameKo())
                .nameEn(artist.getNameEn())
                .build();
    }
}