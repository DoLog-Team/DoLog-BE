package com.dolog.server.domain.exhibition.repository.projection;

import java.util.UUID;

public interface ArtistArtworkCountProjection {

    UUID getArtistId();

    Long getArtworkCount();
}
