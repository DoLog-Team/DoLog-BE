package com.dolog.server.domain.artwork.support;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.service.order.ArtworkOrderAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ArtworkOrderHandler {

    private final ArtworkOrderAdapter artworkOrderAdapter;

    public void apply(
            Artwork artwork,
            Integer prev,
            Integer next,
            UUID zoneId
    ) {

        if ((prev == null && next != null)
                || (prev != null && next == null)) {

            throw new ArtworkException(
                    ArtworkErrorCode.INVALID_ORDER_REQUEST
            );
        }

        if (zoneId != null) {

            artworkOrderAdapter.moveZone(
                    artwork,
                    zoneId,
                    prev,
                    next
            );

            return;
        }

        if (prev != null && next != null) {

            artworkOrderAdapter.reorder(
                    artwork,
                    prev,
                    next,
                    artwork.getExhibitionZone().getId()
            );
        }
    }
}