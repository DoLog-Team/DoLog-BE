package com.dolog.server.domain.exhibition.web.dto.response.artist;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ExhibitionArtistListResponse {

    private UUID artistId;
    private UUID profileId;
    private String nameKo;
    private String nameEn;
    private String profileImg;
    private Boolean isPublic;
}