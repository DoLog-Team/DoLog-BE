package com.dolog.server.domain.artist.repository;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistListResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArtistProfileRepository extends JpaRepository<ArtistProfile, UUID> {
    boolean existsByArtistAndExhibition(Artist artist, Exhibition exhibition);
    boolean existsByArtistId(UUID artistId);

    @Query("SELECT p FROM ArtistProfile p " +
            "LEFT JOIN FETCH p.snsList " +
            "LEFT JOIN FETCH p.artworkArtistMaps m " +
            "LEFT JOIN FETCH m.artwork " +
            "WHERE p.id = :profileId")
    Optional<ArtistProfile> findByIdWithDetails(@Param("profileId") UUID profileId);

    // 전시 ID로 프로필 목록 찾기
    @Query("""
        SELECT p
        FROM ArtistProfile p
        JOIN ExhibitionArtistMap m
          ON m.artist = p.artist
         AND m.exhibition = p.exhibition
        WHERE p.exhibition.id = :exhibitionId
          AND m.status =
              com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus.JOINED
        """)
    List<ArtistProfile> findAllByExhibitionId(
            @Param("exhibitionId") UUID exhibitionId
    );

    Optional<ArtistProfile> findByArtistAndExhibition(Artist artist, Exhibition exhibition);

    @Query("""
            SELECT p.artist.id
            FROM ArtistProfile p
            WHERE p.exhibition.id = :exhibitionId
              AND p.artist.id IN :artistIds
            """)
    List<UUID> findArtistIdsByExhibitionIdAndArtistIdIn(
            @Param("exhibitionId") UUID exhibitionId,
            @Param("artistIds") Collection<UUID> artistIds
    );

    @Query("""
        SELECT p
        FROM ArtistProfile p
        JOIN ExhibitionArtistMap m
          ON m.artist = p.artist
         AND m.exhibition = p.exhibition
        WHERE p.exhibition.id = :exhibitionId
          AND m.status =
              com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus.JOINED
          AND p.nameKo < :nameKo
        ORDER BY p.nameKo DESC
        """)
    List<ArtistProfile> findPrevProfile(
            @Param("exhibitionId") UUID exhibitionId,
            @Param("nameKo") String nameKo,
            Pageable pageable
    );

    @Query("""
        SELECT p
        FROM ArtistProfile p
        JOIN ExhibitionArtistMap m
          ON m.artist = p.artist
         AND m.exhibition = p.exhibition
        WHERE p.exhibition.id = :exhibitionId
          AND m.status =
              com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus.JOINED
          AND p.nameKo > :nameKo
        ORDER BY p.nameKo ASC
        """)
    List<ArtistProfile> findNextProfile(
            @Param("exhibitionId") UUID exhibitionId,
            @Param("nameKo") String nameKo,
            Pageable pageable
    );

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("update ArtistProfile e set e.deletedAt = :at, e.updatedAt = :at where e.artist.id = :id and e.deletedAt is null")
    void hideByArtistId(@org.springframework.data.repository.query.Param("id") java.util.UUID id,
            @org.springframework.data.repository.query.Param("at") java.time.LocalDateTime at);

}
