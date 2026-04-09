package com.dolog.server.domain.artist.web.dto.response;

import com.dolog.server.domain.artist.entity.ArtistSns;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ArtistSnsResponse {
    private UUID snsId;
    private String platformName;
    private String url;

    public static ArtistSnsResponse from(ArtistSns sns) {
        return ArtistSnsResponse.builder()
                .snsId(sns.getId())
                .platformName(sns.getPlatformName())
                .url(sns.getUrl())
                .build();
    }
}
