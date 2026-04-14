package com.dolog.server.domain.artist.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class ArtistProfileListResponse {
    private int total;
    private List<ArtistProfileResponse> artistProfiles;
}
