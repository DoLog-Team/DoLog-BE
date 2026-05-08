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

    private UUID exhibitionId;
    private String slug;
    private String exhibitionTitle;
    private String artistName;

    private UUID zoneId;
    private String zoneName;
    private Integer orderIndex;
}
