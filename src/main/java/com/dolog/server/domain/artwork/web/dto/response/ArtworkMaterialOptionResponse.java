package com.dolog.server.domain.artwork.web.dto.response;

import com.dolog.server.domain.artwork.entity.ArtworkMaterialOption;

import java.util.UUID;

public record ArtworkMaterialOptionResponse(UUID id, String name, Integer orderIndex) {

    public static ArtworkMaterialOptionResponse from(ArtworkMaterialOption option) {
        return new ArtworkMaterialOptionResponse(option.getId(), option.getName(), option.getOrderIndex());
    }
}
