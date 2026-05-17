package com.dolog.server.domain.artwork.service.artwork.command;

import com.dolog.server.domain.artwork.web.dto.request.ArtworkCreateRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkUpdateFullRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkUpdateRequest;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkCreateResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkUpdateFullResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtworkCommandService {

    private final ArtworkCreateProcessor artworkCreateProcessor;
    private final ArtworkUpdateProcessor artworkUpdateProcessor;
    private final ArtworkDeleteProcessor artworkDeleteProcessor;
    private final ArtworkOrderProcessor artworkOrderProcessor;

    public ArtworkCreateResponse createArtwork(
            ArtworkCreateRequest request
    ) {

        return artworkCreateProcessor.execute(request);
    }

    public ArtworkCreateResponse updateArtwork(
            UUID artworkId,
            ArtworkUpdateRequest request
    ) {

        return artworkUpdateProcessor.update(
                artworkId,
                request
        );
    }

    public ArtworkUpdateFullResponse updateArtworkFull(
            UUID artworkId,
            ArtworkUpdateFullRequest request
    ) {

        return artworkUpdateProcessor.updateFull(
                artworkId,
                request
        );
    }

    public void deleteArtwork(
            UUID artworkId
    ) {

        artworkDeleteProcessor.delete(
                artworkId
        );
    }

    public void reorderArtwork(
            UUID artworkId,
            Integer prev,
            Integer next
    ) {

        artworkOrderProcessor.reorder(
                artworkId,
                prev,
                next
        );
    }

    public void moveArtworkZone(
            UUID artworkId,
            UUID zoneId,
            Integer prev,
            Integer next
    ) {

        artworkOrderProcessor.moveZone(
                artworkId,
                zoneId,
                prev,
                next
        );
    }
}