package com.dolog.server.domain.exhibition.web.dto.request.guideMap;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class ExhibitionGuideMapCreateRequest {

    private UUID zoneId;

    private MultipartFile image;

    private String description;
}
