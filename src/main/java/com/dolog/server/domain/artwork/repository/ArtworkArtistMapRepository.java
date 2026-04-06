package com.dolog.server.domain.artwork.repository;

import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ArtworkArtistMapRepository extends JpaRepository<ArtworkArtistMap, Long> {

    @Query("SELECT aam FROM ArtworkArtistMap aam " +
            "JOIN FETCH aam.artist " +
            "WHERE aam.artwork.id IN :artworkIds")
    List<ArtworkArtistMap> findByArtworkIdIn(@Param("artworkIds") List<UUID> artworkIds);
}
