package com.dolog.server.domain.artwork.web.dto.response;

import com.dolog.server.domain.artwork.entity.ArtworkCategory;

import java.util.UUID;

public record ArtworkCategoryResponse(UUID id, String name, Integer orderIndex) {

    public static ArtworkCategoryResponse from(ArtworkCategory category) {
        return new ArtworkCategoryResponse(category.getId(), category.getName(), category.getOrderIndex());
    }
}
