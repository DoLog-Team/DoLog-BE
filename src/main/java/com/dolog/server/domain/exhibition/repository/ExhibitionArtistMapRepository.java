package com.dolog.server.domain.exhibition.repository;

import com.dolog.server.domain.exhibition.entity.ExhibitionArtistMap;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.domain.exhibition.repository.projection.ArtistArtworkCountProjection;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistItemResponse;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExhibitionArtistMapRepository extends JpaRepository<ExhibitionArtistMap, UUID> {

    boolean existsByExhibitionIdAndArtistId(UUID exhibitionId, UUID artistId);
    boolean existsByExhibitionIdAndArtistIdAndStatus(
            UUID exhibitionId,
            UUID artistId,
            ExhibitionArtistStatus status
    );
    List<ExhibitionArtistMap> findByExhibitionId(UUID exhibitionId);
    Optional<ExhibitionArtistMap> findByExhibitionIdAndArtistId(UUID exhibitionId, UUID artistId);
    boolean existsByArtistId(UUID artistId);
    long countByExhibitionIdAndStatus(UUID exhibitionId, ExhibitionArtistStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"artist", "artist.account"})
    List<ExhibitionArtistMap> findAllByExhibitionIdAndArtistIdIn(
            UUID exhibitionId,
            Collection<UUID> artistIds
    );

    @Query("""
        SELECT new com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistItemResponse(
            p.id,
            a.id,
            a.nameKo,
            a.nameEn,
            p.profileImg
        )
        FROM ExhibitionArtistMap m
        JOIN m.artist a
        JOIN ArtistProfile p
            ON p.artist = a AND p.exhibition.id = :exhibitionId
        WHERE m.exhibition.id = :exhibitionId
                AND m.status = com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus.JOINED
                AND p.isPublic = true
        """)
    List<ExhibitionArtistItemResponse> findArtists(UUID exhibitionId);

    @Query("""
        SELECT m.artist.id
        FROM ExhibitionArtistMap m
        WHERE m.exhibition.id = :exhibitionId
          AND m.artist.id IN :artistIds
          AND m.status =
              com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus.JOINED
        """)
    List<UUID> findJoinedArtistIds(
            @Param("exhibitionId") UUID exhibitionId,
            @Param("artistIds") Collection<UUID> artistIds
    );

    @Query("""
            SELECT DISTINCT m
            FROM ExhibitionArtistMap m
            JOIN FETCH m.exhibition e
            JOIN FETCH e.exhibitionDetail d
            LEFT JOIN FETCH e.exhibitionMap em
            JOIN ArtistProfile p
              ON p.artist = m.artist
             AND p.exhibition = e
            WHERE m.artist.id = :artistId
              AND m.status =
                  com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus.JOINED
              AND e.isPublic = true
              AND p.isPublic = true
            ORDER BY d.startDate DESC, e.id DESC
            """)
    List<ExhibitionArtistMap> findPublicJoinedExhibitionsByArtistId(
            @Param("artistId") UUID artistId
    );

    @EntityGraph(attributePaths = {"artist", "artist.account"})
    @Query(
            value = """
            SELECT m
            FROM ExhibitionArtistMap m
            JOIN m.artist a
            LEFT JOIN a.account acc
            WHERE m.exhibition.id = :exhibitionId
              AND m.status = :status
              AND (
                  :search IS NULL
                  OR LOWER(a.nameKo) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                  OR LOWER(a.nameEn) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                  OR LOWER(acc.email) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
              )
            ORDER BY m.createdAt DESC, m.id DESC
            """,
            countQuery = """
            SELECT COUNT(m)
            FROM ExhibitionArtistMap m
            JOIN m.artist a
            LEFT JOIN a.account acc
            WHERE m.exhibition.id = :exhibitionId
              AND m.status = :status
              AND (
                  :search IS NULL
                  OR LOWER(a.nameKo) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                  OR LOWER(a.nameEn) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                  OR LOWER(acc.email) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
              )
            """
    )
    Page<ExhibitionArtistMap> findArtistsForManagement(
            @Param("exhibitionId") UUID exhibitionId,
            @Param("status") ExhibitionArtistStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
    SELECT
        aam.artist.id AS artistId,
        COUNT(DISTINCT aam.artwork.id) AS artworkCount
    FROM ArtworkArtistMap aam
    WHERE aam.artwork.exhibition.id = :exhibitionId
      AND aam.artist.id IN :artistIds
    GROUP BY aam.artist.id
    """)
    List<ArtistArtworkCountProjection> countArtworksByArtistIds(
            @Param("exhibitionId") UUID exhibitionId,
            @Param("artistIds") Collection<UUID> artistIds
    );

    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true)
    @Query("update ExhibitionArtistMap m set m.status = :target, m.updatedAt = :at "
            + "where m.artist.id = :artistId and m.status in :sources")
    void updateStatusesForWithdrawal(@Param("artistId") UUID artistId,
            @Param("sources") Collection<ExhibitionArtistStatus> sources,
            @Param("target") ExhibitionArtistStatus target,
            @Param("at") java.time.LocalDateTime at);

}
