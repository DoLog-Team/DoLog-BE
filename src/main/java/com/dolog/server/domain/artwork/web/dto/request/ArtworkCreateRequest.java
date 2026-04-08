package com.dolog.server.domain.artwork.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ArtworkCreateRequest {
    @NotBlank(message = "작품 제목은 필수입니다.")
    private String title;

    private String category;
    private String material;
    private String size;
    private String description;

    @NotNull(message = "전시회 ID는 필수입니다.")
    private UUID exhibitionId; // exhibition_id 제거

    private UUID zoneId;       // zone_id 제거

    private String mainImage;   // main_image 제거

    private String purchaseUrl; // purchase_url 제거

    private Integer orderIndex; // order_index 제거
}