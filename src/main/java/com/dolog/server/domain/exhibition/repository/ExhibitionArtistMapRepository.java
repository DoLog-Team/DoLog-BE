package com.dolog.server.domain.exhibition.repository;

import com.dolog.server.domain.exhibition.entity.ExhibitionArtistMap;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistListResponse;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExhibitionArtistMapRepository extends JpaRepository<ExhibitionArtistMap, UUID> {

    boolean existsByExhibitionIdAndArtistId(UUID exhibitionId, UUID artistId);
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
}
