package com.dolog.server.domain.artwork.web.dto.request;

import com.dolog.server.domain.artwork.entity.enums.ArtworkVisibilityOption;
import jakarta.validation.constraints.NotNull;

public record ArtworkVisibilityUpdateRequest(
        @NotNull ArtworkVisibilityOption visibility
) {}
