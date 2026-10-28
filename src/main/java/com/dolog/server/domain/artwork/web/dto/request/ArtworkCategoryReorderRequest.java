package com.dolog.server.domain.artwork.web.dto.request;

import java.util.UUID;

public record ArtworkCategoryReorderRequest(UUID categoryId, Integer order) {
}
