package com.dolog.server.domain.exhibition.web.dto.response.artist;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class ExhibitionArtistItemResponse {

    private UUID profileId;
    private UUID artistId;
    private String nameKo;
    private String nameEn;
    private String profileImg;
}
