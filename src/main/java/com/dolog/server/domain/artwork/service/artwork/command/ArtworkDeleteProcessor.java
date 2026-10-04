package com.dolog.server.domain.artwork.service.artwork.command;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.support.ArtworkValidator;
import com.dolog.server.domain.bts.repository.BtsArtworkMapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtworkDeleteProcessor {

    private final ArtworkRepository artworkRepository;
    private final ArtworkValidator artworkValidator;
    private final BtsArtworkMapRepository btsArtworkMapRepository;

    public void delete(
            UUID accountId,
            UUID artworkId
    ) {

        Artist artist = artworkValidator.getLoginArtist(accountId);
        Artwork artwork = artworkValidator.getOwnedArtwork(artworkId, artist);

        // bts_artwork_map 은 작품 쪽 cascade 가 없어서 FK 위반이 나지 않게 먼저 지운다.
        btsArtworkMapRepository.deleteAllByArtworkId(artworkId);
        artworkRepository.delete(artwork);
    }
}
