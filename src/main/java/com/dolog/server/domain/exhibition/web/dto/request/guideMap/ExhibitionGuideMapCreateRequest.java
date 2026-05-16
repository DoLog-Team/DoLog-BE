package com.dolog.server.domain.exhibition.web.dto.request.guideMap;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import java.util.List;

@Getter
@NoArgsConstructor
public class ExhibitionGuideMapCreateRequest {

    private List<GuideMapRequest> requests;

    @Getter
    @NoArgsConstructor
    public static class GuideMapRequest {

        private UUID zoneId;
        private MultipartFile image;
        private String description;
    }
}
