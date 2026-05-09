package com.dolog.server.domain.exhibition.repository;

import com.dolog.server.domain.exhibition.entity.ExhibitionArtistMap;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.domain.exhibition.web.dto.response.artist.ExhibitionArtistListResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExhibitionArtistMapRepository extends JpaRepository<ExhibitionArtistMap, UUID> {

    boolean existsByExhibitionIdAndArtistId(UUID exhibitionId, UUID artistId);
    List<ExhibitionArtistMap> findByExhibitionId(UUID exhibitionId);
    Optional<ExhibitionArtistMap> findByExhibitionIdAndArtistId(UUID exhibitionId, UUID artistId);

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
        """)
    List<ExhibitionArtistListResponse> findArtists(UUID exhibitionId);
}