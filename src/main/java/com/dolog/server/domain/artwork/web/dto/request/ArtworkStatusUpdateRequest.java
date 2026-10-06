package com.dolog.server.domain.artwork.web.dto.request;

import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import jakarta.validation.constraints.NotNull;

public record ArtworkStatusUpdateRequest(
        @NotNull ArtworkStatus status
) {}
