package com.dolog.server.domain.exhibition.web.dto.request.guideMap;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Data
@Getter
@Setter
@NoArgsConstructor
public class ExhibitionGuideMapCreateRequest {

    private UUID zoneId;

    private MultipartFile image;

    private String description;

    private Boolean isDeleteImg;
}
