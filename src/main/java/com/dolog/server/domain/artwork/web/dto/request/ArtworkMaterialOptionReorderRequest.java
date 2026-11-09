package com.dolog.server.domain.artwork.web.dto.request;

import java.util.UUID;

public record ArtworkMaterialOptionReorderRequest(UUID materialOptionId, Integer order) {
}
