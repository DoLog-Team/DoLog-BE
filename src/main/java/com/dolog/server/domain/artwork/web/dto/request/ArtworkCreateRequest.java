package com.dolog.server.domain.artwork.web.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("exhibition_id")
    private UUID exhibitionId;

    @JsonProperty("zone_id")
    private UUID zoneId;

    @JsonProperty("main_image")
    private String mainImage;
    @JsonProperty("purchase_url")
    private String purchaseUrl;
    @JsonProperty("order_index")
    private Integer orderIndex;
}