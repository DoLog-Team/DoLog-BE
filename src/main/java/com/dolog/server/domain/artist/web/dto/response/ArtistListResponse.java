package com.dolog.server.domain.artist.web.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ArtistListResponse {

    private List<ArtistListItemResponse> artists;
    private long totalElements;
    private int totalPages;
}