package com.dolog.server.domain.like.repository;

import com.dolog.server.domain.like.entity.ArtistProfileLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ArtistProfileLikeRepository extends JpaRepository<ArtistProfileLike, UUID> {

    boolean existsByArtistProfileIdAndVisitorId(UUID artistProfileId, String visitorId);

    Optional<ArtistProfileLike> findByArtistProfileIdAndVisitorId(UUID artistProfileId, String visitorId);

    long countByArtistProfileId(UUID artistProfileId);
}
