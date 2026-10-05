package com.dolog.server.domain.artwork.web.dto.request;

import jakarta.validation.constraints.NotNull;

public record ArtworkHiddenUpdateRequest(
        @NotNull Boolean hidden
) {}
