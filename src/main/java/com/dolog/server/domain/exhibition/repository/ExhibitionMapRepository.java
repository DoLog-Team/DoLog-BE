package com.dolog.server.domain.exhibition.repository;

import com.dolog.server.domain.exhibition.entity.ExhibitionMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExhibitionMapRepository extends JpaRepository<ExhibitionMap, UUID> {

    boolean existsByExhibitionId(UUID exhibitionId);
}
