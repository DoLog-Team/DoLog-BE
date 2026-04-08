package com.dolog.server.domain.artwork.web.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
public class ArtworkUpdateRequest {
    private String title;
    private String category;
    private String material;
    private String size;
    private String description;
    private String mainImage;
    private String purchaseUrl;
    private Integer orderIndex;
    private UUID zoneId; // 구역 이동이 있을 수 있으니 포함
}