package com.dolog.server.domain.artist.service;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArtistProfileProvisionService {

    private final ArtistProfileRepository artistProfileRepository;

    @Transactional
    public ArtistProfile ensureProfile(Artist artist) {
        return artistProfileRepository.findByArtistId(artist.getId())
                .orElseGet(() -> {
                    ArtistProfile profile = ArtistProfile.builder()
                            .artist(artist)
                            .build();
                    profile.fillDefaultInfoFromArtist();
                    return artistProfileRepository.save(profile);
                });
    }
}
