package com.dolog.server.domain.artist.repository;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArtistProfileRepository extends JpaRepository<ArtistProfile, UUID> {
    boolean existsByArtistAndExhibition(Artist artist, Exhibition exhibition);

    @Query("SELECT p FROM ArtistProfile p " +
            "LEFT JOIN FETCH p.snsList " +
            "LEFT JOIN FETCH p.artworkArtistMaps m " +
            "LEFT JOIN FETCH m.artwork " +
            "WHERE p.id = :profileId")
    Optional<ArtistProfile> findByIdWithDetails(@Param("profileId") UUID profileId);

    // 전시 ID로 프로필 목록 찾기
    List<ArtistProfile> findAllByExhibitionId(UUID exhibitionId);

    // 이 메서드를 추가해야 합니다.
    Optional<ArtistProfile> findByArtistAndExhibition(Artist artist, Exhibition exhibition);

}
