package com.dolog.server.domain.artwork.service.order;

import java.util.UUID;

public interface ArtworkOrderService {
    void reorderArtworkIndices(UUID exhibitionId);
}
