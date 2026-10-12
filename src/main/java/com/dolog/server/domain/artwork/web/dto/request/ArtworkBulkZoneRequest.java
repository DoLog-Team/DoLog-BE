package com.dolog.server.domain.artwork.web.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ArtworkBulkZoneRequest(
        @NotEmpty @Size(max = 100) List<@NotNull UUID> artworkIds,
        @NotNull UUID zoneId
) {}
