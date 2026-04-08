package com.dolog.server.domain.artwork.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ArtworkListResponse {
    private UUID id;
    private String title;
    private String category;
    private String imageUrl;
    private String exhibitionTitle;
    private String artistName;
}
