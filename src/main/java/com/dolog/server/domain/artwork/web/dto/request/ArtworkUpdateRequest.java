package com.dolog.server.domain.artwork.web.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ArtworkUpdateRequest {
    private UUID artistProfileId;
    private String title;
    private String category;
    private String material;
    private String size;
    private String description;
    private String purchaseUrl;
    private Integer orderIndex;

    private UUID zoneId;
    private Integer prevOrder;
    private Integer nextOrder;

    private String artistRole;
    private MultipartFile mainImageFile;
    private MultipartFile locationMapFile;
}