package com.dolog.server.domain.exhibition.web.dto.request.guideMap;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ExhibitionGuideMapCreateRequest {
    private String imageUrl;
    private String description;
}
