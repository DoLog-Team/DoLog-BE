package com.dolog.server.domain.artwork.repository;

import com.dolog.server.domain.artwork.entity.Artwork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ArtworkRepository extends JpaRepository<Artwork, UUID> {

    @Query("SELECT DISTINCT a FROM Artwork a " +
            "JOIN FETCH a.exhibition e " +
            "LEFT JOIN ExhibitionDetail ed ON ed.exhibition = e " +
            "LEFT JOIN ArtworkArtistMap aam ON aam.artwork = a " +
            "LEFT JOIN Artist art ON aam.artist = art " +
            "WHERE (:category IS NULL OR a.category = :category) " +
            "AND (:search IS NULL OR (a.title LIKE CONCAT('%', :search, '%') OR art.nameKo LIKE CONCAT('%', :search, '%')))")
    List<Artwork> searchArtworks(@Param("category") String category, @Param("search") String search);
}
