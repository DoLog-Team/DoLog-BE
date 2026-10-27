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
        ArtistProfile profile = artistProfileRepository.findByArtistId(artist.getId())
                .orElseGet(() -> {
                    ArtistProfile created = ArtistProfile.builder()
                            .artist(artist)
                            .build();
                    created.fillDefaultInfoFromArtist();
                    return artistProfileRepository.save(created);
                });
        artist.linkProfile(profile);
        return profile;
    }
}
