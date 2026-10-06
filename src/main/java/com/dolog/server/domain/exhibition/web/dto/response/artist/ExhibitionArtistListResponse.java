package com.dolog.server.domain.exhibition.web.dto.response.artist;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ExhibitionArtistListResponse {

    private List<ExhibitionArtistItemResponse> artists;
    private int totalCount;

    public static ExhibitionArtistListResponse from(
            List<ExhibitionArtistItemResponse> artists
    ) {
        return new ExhibitionArtistListResponse(
                List.copyOf(artists),
                artists.size()
        );
    }
}
