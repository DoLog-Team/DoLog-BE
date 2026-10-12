package com.dolog.server.domain.artwork.web.dto.response;

import java.util.List;
import java.util.UUID;

public record ArtworkBulkZoneResponse(
        UUID zoneId,
        String zoneName,
        int changedCount,
        List<UUID> artworkIds
) {}
