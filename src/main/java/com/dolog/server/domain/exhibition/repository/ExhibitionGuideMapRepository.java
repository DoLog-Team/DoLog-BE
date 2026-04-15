package com.dolog.server.domain.exhibition.repository;

import com.dolog.server.domain.exhibition.entity.ExhibitionGuideMap;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ExhibitionGuideMapRepository extends JpaRepository<ExhibitionGuideMap, UUID> {
    List<ExhibitionGuideMap> findByExhibitionId(UUID exhibitionId);

    List<ExhibitionGuideMap> findByZoneId(UUID zoneId);
}
