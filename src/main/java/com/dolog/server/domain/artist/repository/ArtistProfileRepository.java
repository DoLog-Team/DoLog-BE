package com.dolog.server.domain.artist.repository;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.repository.projection.ArtistProfileListItemProjection;
import com.dolog.server.domain.exhibition.entity.Exhibition;
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

    @Query(value = """
            SELECT BIN_TO_UUID(p.id) AS profileId,
                   BIN_TO_UUID(p.artist_id) AS artistId,
                   p.name_ko AS nameKo,
                   p.name_en AS nameEn,
                   p.profile_img AS profileImg,
                   p.is_public AS isPublic,
                   p.view_count AS viewCount,
                   COUNT(l.id) AS likeCount
            FROM artist_profiles p
            JOIN exhibition_artist_map m
              ON m.exhibition_id = p.exhibition_id
             AND m.artist_id = p.artist_id
             AND m.status = 'JOINED'
            LEFT JOIN artist_profile_likes l
              ON l.artist_profile_id = p.id
            WHERE p.exhibition_id = :exhibitionId
            GROUP BY p.id,
                     p.artist_id,
                     p.name_ko,
                     p.name_en,
                     p.profile_img,
                     p.is_public,
                     p.view_count
            ORDER BY p.name_ko ASC, p.id ASC
            """, nativeQuery = true)
    List<ArtistProfileListItemProjection> findListItemsByExhibitionId(
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
}
