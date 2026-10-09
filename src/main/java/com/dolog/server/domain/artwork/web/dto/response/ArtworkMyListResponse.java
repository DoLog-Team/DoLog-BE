package com.dolog.server.domain.artwork.web.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ArtworkMyListResponse {

    private List<ArtworkMyItemResponse> artworks;
    private long totalElements;
    private int totalPages;
}
