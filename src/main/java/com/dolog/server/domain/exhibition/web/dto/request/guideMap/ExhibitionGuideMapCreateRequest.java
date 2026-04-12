package com.dolog.server.domain.exhibition.web.dto.request.guideMap;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class ExhibitionGuideMapCreateRequest {
    private UUID zoneId;
    private String imageUrl;
    private String description;
}
