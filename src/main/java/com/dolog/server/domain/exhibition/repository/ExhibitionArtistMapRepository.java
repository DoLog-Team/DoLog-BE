package com.dolog.server.domain.exhibition.repository;

import com.dolog.server.domain.exhibition.entity.ExhibitionArtistMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExhibitionArtistMapRepository extends JpaRepository<ExhibitionArtistMap, UUID> {

    boolean existsByExhibitionIdAndArtistId(UUID exhibitionId, UUID artistId);
}