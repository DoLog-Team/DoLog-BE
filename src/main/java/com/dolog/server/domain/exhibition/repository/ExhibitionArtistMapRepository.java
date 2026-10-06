package com.dolog.server.domain.exhibition.repository;

import com.dolog.server.domain.exhibition.entity.ExhibitionArtistMap;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistListResponse;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistManageItemResponse;
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
    List<ExhibitionArtistMap> findByExhibitionId(UUID exhibitionId);
    Optional<ExhibitionArtistMap> findByExhibitionIdAndArtistId(UUID exhibitionId, UUID artistId);
    boolean existsByArtistId(UUID artistId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"artist", "artist.account"})
    List<ExhibitionArtistMap> findAllByExhibitionIdAndArtistIdIn(
            UUID exhibitionId,
            Collection<UUID> artistIds
    );

    @Query("""
        SELECT new com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistListResponse(
            a.id,
            p.id,
            a.nameKo,
            a.nameEn,
            p.profileImg,
            p.isPublic
        )
        FROM ExhibitionArtistMap m
        JOIN m.artist a
        LEFT JOIN ArtistProfile p
            ON p.artist = a AND p.exhibition.id = :exhibitionId
        WHERE m.exhibition.id = :exhibitionId
                AND m.status = com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus.JOINED
        """)
    List<ExhibitionArtistListResponse> findArtists(UUID exhibitionId);

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

    @Query(
            value = """
                SELECT new com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistManageItemResponse(
                    a.id,
                    a.nameKo,
                    acc.email,
                    m.greeting,
                    CASE
                        WHEN m.status = com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus.JOINED
                        THEN COUNT(DISTINCT aam.artwork.id)
                        ELSE NULL
                    END
                )
                FROM ExhibitionArtistMap m
                JOIN m.artist a
                LEFT JOIN a.account acc
                LEFT JOIN ArtworkArtistMap aam
                    ON aam.artist = a
                    AND aam.artwork.exhibition.id = :exhibitionId
                WHERE m.exhibition.id = :exhibitionId
                  AND (:status IS NULL OR m.status = :status)
                  AND (
                      :search IS NULL
                      OR LOWER(a.nameKo) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                      OR LOWER(a.nameEn) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                      OR LOWER(acc.email) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                  )
                GROUP BY m.id, a.id, a.nameKo, acc.email, m.greeting, m.status, m.createdAt
                ORDER BY m.createdAt DESC, m.id DESC
                """,
            countQuery = """
                SELECT COUNT(m)
                FROM ExhibitionArtistMap m
                JOIN m.artist a
                LEFT JOIN a.account acc
                WHERE m.exhibition.id = :exhibitionId
                  AND (:status IS NULL OR m.status = :status)
                  AND (
                      :search IS NULL
                      OR LOWER(a.nameKo) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                      OR LOWER(a.nameEn) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                      OR LOWER(acc.email) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\'
                  )
                """
    )
    Page<ExhibitionArtistManageItemResponse> findArtistsForManagement(
            @Param("exhibitionId") UUID exhibitionId,
            @Param("status") ExhibitionArtistStatus status,
            @Param("search") String search,
            Pageable pageable
    );
}
