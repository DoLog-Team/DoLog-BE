package com.dolog.server.domain.artwork.repository;

import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArtworkArtistMapRepository extends JpaRepository<ArtworkArtistMap, Long> {

    @Query("SELECT aam FROM ArtworkArtistMap aam " +
            "JOIN FETCH aam.artist " +
            "WHERE aam.artwork.id IN :artworkIds")
    List<ArtworkArtistMap> findByArtworkIdIn(@Param("artworkIds") List<UUID> artworkIds);

    // 작품 ID와 아티스트 프로필 ID 조합으로 찾기
    Optional<ArtworkArtistMap> findByArtworkIdAndArtistProfileId(UUID artworkId, UUID artistProfileId);

    // 중복 등록 확인
    boolean existsByArtworkIdAndArtistProfileId(UUID artworkId, UUID artistProfileId);

    boolean existsByArtistId(UUID artistId);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update ArtworkArtistMap m set m.deletedAt = :at, m.updatedAt = :at where m.artwork.id = :id and m.deletedAt is null")
    void hideByArtworkId(@org.springframework.data.repository.query.Param("id") java.util.UUID id,
                        @org.springframework.data.repository.query.Param("at") java.time.LocalDateTime at);


    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("update ArtworkArtistMap e set e.deletedAt = :at, e.updatedAt = :at where e.artist.id = :id and e.deletedAt is null")
    void hideByArtistId(@org.springframework.data.repository.query.Param("id") java.util.UUID id,
            @org.springframework.data.repository.query.Param("at") java.time.LocalDateTime at);

}
