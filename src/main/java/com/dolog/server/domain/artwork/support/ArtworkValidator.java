package com.dolog.server.domain.artwork.support;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.exception.artistError.ArtistNotFoundException;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
@Component
@RequiredArgsConstructor
public class ArtworkValidator {

    private final ArtistRepository artistRepository;
    private final ArtworkRepository artworkRepository;
    private final ExhibitionZoneRepository exhibitionZoneRepository;
    private final ExhibitionRepository exhibitionRepository;

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

    // 전시 어드민 본인 전시인지 확인한다
    public Exhibition getOwnedExhibition(UUID accountId, UUID exhibitionId) {

        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        if (!exhibition.getAccount().getId().equals(accountId)) {
            throw new ArtworkException(ArtworkErrorCode.NOT_EXHIBITION_OWNER);
        }

        return exhibition;
    }

    // 하나라도 이 전시 출품작이 아니면 전체 거절, 요청 순서대로 돌려준다
    public List<Artwork> getArtworksInExhibition(UUID exhibitionId, List<UUID> artworkIds) {

        List<UUID> ids = artworkIds.stream().distinct().toList();
        Map<UUID, Artwork> found = artworkRepository.findByExhibitionIdAndIdIn(exhibitionId, ids).stream()
                .collect(Collectors.toMap(Artwork::getId, Function.identity()));

        if (found.size() != ids.size()) {
            throw new ArtworkException(ArtworkErrorCode.NOT_IN_EXHIBITION);
        }

        return ids.stream().map(found::get).toList();
    }
}
