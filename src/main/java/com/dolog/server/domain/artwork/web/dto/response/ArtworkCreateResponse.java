package com.dolog.server.domain.artwork.web.dto.response;

import com.dolog.server.domain.artwork.entity.Artwork;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class ArtworkCreateResponse {
    private UUID id;
    private String title;
    private String category;
    private String material;
    private String size;
    private String description;
    private String mainImage;
    private String purchaseUrl;
    private Integer orderIndex;
    private UUID exhibitionId;
    private String artistName; // 등록된 작가 이름

    // Static Factory Method
    public static ArtworkCreateResponse of(Artwork artwork, String artistName) {
        return ArtworkCreateResponse.builder()
                .id(artwork.getId())
                .title(artwork.getTitle())
                .category(artwork.getCategory())
                .material(artwork.getMaterial())
                .size(artwork.getSize())
                .description(artwork.getDescription())
                .mainImage(artwork.getMainImg())
                .purchaseUrl(artwork.getPurchaseUrl())
                .orderIndex(artwork.getOrderIndex())
                .exhibitionId(artwork.getExhibition().getId())
                .artistName(artistName)
                .build();
    }
}