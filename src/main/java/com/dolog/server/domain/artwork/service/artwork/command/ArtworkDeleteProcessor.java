package com.dolog.server.domain.artwork.service.artwork.command;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtworkDeleteProcessor {

    private final ArtworkRepository artworkRepository;

    public void delete(
            UUID artworkId
    ) {

        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() ->
                        new ArtworkException(
                                ArtworkErrorCode.ARTWORK_NOT_FOUND
                        ));

        artworkRepository.delete(artwork);
    }
}