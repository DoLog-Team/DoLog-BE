package com.dolog.server.domain.exhibition.web.dto.response.artist;

import lombok.Builder;
import lombok.Getter;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.util.UUID;

@Getter
@Builder
public class ExhibitionArtistListResponse {

    private UUID artistId;
    private String nameKo;
    private String nameEn;
    private String profileImg;
    private Boolean isPublic;
}