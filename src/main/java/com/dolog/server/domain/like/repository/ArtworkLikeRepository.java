package com.dolog.server.domain.like.repository;

import com.dolog.server.domain.like.entity.ArtworkLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ArtworkLikeRepository extends JpaRepository<ArtworkLike, UUID> {

    boolean existsByArtworkIdAndVisitorId(UUID artworkId, String visitorId);

    Optional<ArtworkLike> findByArtworkIdAndVisitorId(UUID artworkId, String visitorId);

    long countByArtworkId(UUID artworkId);

    void deleteAllByArtworkId(UUID artworkId);
}
