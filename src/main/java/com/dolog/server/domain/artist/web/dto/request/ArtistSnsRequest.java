package com.dolog.server.domain.artist.web.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ArtistSnsRequest {
    private String platformName;
    private String url;
}
