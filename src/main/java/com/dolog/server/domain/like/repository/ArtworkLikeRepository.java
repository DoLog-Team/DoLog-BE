package com.dolog.server.domain.like.repository;

import com.dolog.server.domain.like.entity.ArtworkLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ArtworkLikeRepository extends JpaRepository<ArtworkLike, UUID> {

    boolean existsByArtworkIdAndVisitorId(UUID artworkId, String visitorId);

    Optional<ArtworkLike> findByArtworkIdAndVisitorId(UUID artworkId, String visitorId);

    long countByArtworkId(UUID artworkId);

    // 좋아요는 많을 수 있어 한 번에 지운다. 같은 트랜잭션에 남은 좋아요 엔티티와 꼬이지 않게 컨텍스트를 비운다.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM ArtworkLike l WHERE l.artwork.id = :artworkId")
    void deleteAllByArtworkId(@Param("artworkId") UUID artworkId);
}
