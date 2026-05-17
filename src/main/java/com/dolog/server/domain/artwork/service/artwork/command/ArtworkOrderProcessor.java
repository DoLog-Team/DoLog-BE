package com.dolog.server.domain.artwork.service.artwork.command;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.service.order.ArtworkOrderAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtworkOrderProcessor {

    private final ArtworkRepository artworkRepository;
    private final ArtworkOrderAdapter artworkOrderAdapter;

    public void reorder(
            UUID artworkId,
            Integer prev,
            Integer next
    ) {

        Artwork artwork = getArtwork(artworkId);

        artworkOrderAdapter.reorder(
                artwork,
                prev,
                next,
                artwork.getExhibitionZone().getId()
        );
    }

    public void moveZone(
            UUID artworkId,
            UUID zoneId,
            Integer prev,
            Integer next
    ) {

        Artwork artwork = getArtwork(artworkId);

        artworkOrderAdapter.moveZone(
                artwork,
                zoneId,
                prev,
                next
        );
    }

    private Artwork getArtwork(
            UUID artworkId
    ) {

        return artworkRepository.findById(artworkId)
                .orElseThrow(() ->
                        new ArtworkException(
                                ArtworkErrorCode.ARTWORK_NOT_FOUND
                        ));
    }
}