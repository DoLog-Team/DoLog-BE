package com.dolog.server.domain.artist.repository;

import com.dolog.server.domain.artist.entity.ArtistSns;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ArtistSnsRepository extends JpaRepository<ArtistSns, UUID> {
    List<ArtistSns> findByArtistProfileId(UUID profileId);
}