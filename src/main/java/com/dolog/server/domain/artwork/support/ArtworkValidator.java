package com.dolog.server.domain.artwork.support;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.exception.artistError.ArtistNotFoundException;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.domain.exhibition.repository.ExhibitionZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;
@Component
@RequiredArgsConstructor
public class ArtworkValidator {

    private final ArtistRepository artistRepository;
    private final ArtworkRepository artworkRepository;
    private final ExhibitionZoneRepository exhibitionZoneRepository;

    public Artist getLoginArtist(UUID accountId) {

        return artistRepository.findByAccountId(accountId)
                .orElseThrow(ArtistNotFoundException::new);
    }

    // 다른 작가의 작품은 존재 여부를 숨기기 위해 403 대신 404로 응답한다.
    public Artwork getOwnedArtwork(UUID artworkId, Artist artist) {

        return artworkRepository.findById(artworkId)
                .filter(artwork -> artwork.isLinkedTo(artist.getId()))
                .orElseThrow(() ->
                        new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));
    }

    // 두록 어드민은 모든 작품을, 작가는 자기 작품만 관리한다.
    public Artwork getManagedArtwork(UUID artworkId, UUID accountId, boolean isDologAdmin) {

        if (isDologAdmin) {
            return artworkRepository.findById(artworkId)
                    .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));
        }

        return getOwnedArtwork(artworkId, getLoginArtist(accountId));
    }

    public ExhibitionZone validateZone(
            UUID zoneId,
            UUID exhibitionId
    ) {

        return exhibitionZoneRepository
                .findByIdAndExhibitionId(zoneId, exhibitionId)
                .orElseThrow(() ->
                        new ArtworkException(ArtworkErrorCode.INVALID_EXHIBITION_ZONE));
    }
}
