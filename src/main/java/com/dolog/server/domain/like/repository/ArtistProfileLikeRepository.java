package com.dolog.server.domain.like.repository;

import com.dolog.server.domain.like.entity.ArtistProfileLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ArtistProfileLikeRepository extends JpaRepository<ArtistProfileLike, UUID> {

    boolean existsByArtistProfileIdAndVisitorId(UUID artistProfileId, String visitorId);

    Optional<ArtistProfileLike> findByArtistProfileIdAndVisitorId(UUID artistProfileId, String visitorId);

    long countByArtistProfileId(UUID artistProfileId);

    @Query("""
            SELECT COUNT(l)
            FROM ArtistProfileLike l
            JOIN l.artistProfile p
            WHERE p.artist.id = :artistId
            """)
    long countByArtistId(@Param("artistId") UUID artistId);

    @Query("""
            SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END
            FROM ArtistProfileLike l
            JOIN l.artistProfile p
            WHERE p.artist.id = :artistId
              AND l.visitorId = :visitorId
            """)
    boolean existsByArtistIdAndVisitorId(
            @Param("artistId") UUID artistId,
            @Param("visitorId") String visitorId
    );
}
