package com.dolog.server.domain.artwork.support;

import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.domain.exhibition.repository.ExhibitionZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;
@Component
@RequiredArgsConstructor
public class ArtworkValidator {

    private final ArtistProfileRepository artistProfileRepository;
    private final ExhibitionZoneRepository exhibitionZoneRepository;

    public ArtistProfile validateArtistProfile(UUID profileId) {

        return artistProfileRepository.findById(profileId)
                .orElseThrow(() ->
                        new RuntimeException("Artist Profile not found"));
    }

    public ExhibitionZone validateZone(
            UUID zoneId,
            UUID exhibitionId
    ) {

        if (zoneId == null) {
            throw new RuntimeException("Zone은 필수입니다.");
        }

        return exhibitionZoneRepository
                .findByIdAndExhibitionId(zoneId, exhibitionId)
                .orElseThrow(() ->
                        new RuntimeException("해당 전시에 속한 Zone이 아닙니다."));
    }
}
