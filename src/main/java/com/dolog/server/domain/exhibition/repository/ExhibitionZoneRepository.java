package com.dolog.server.domain.exhibition.repository;

import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExhibitionZoneRepository extends JpaRepository<ExhibitionZone, UUID> {

    List<ExhibitionZone> findByExhibitionId(UUID exhibitionId);

    Optional<ExhibitionZone> findByIdAndExhibitionId(UUID id, UUID exhibitionId);


    // 존 조회 (orderId 기준 오름차순)
    List<ExhibitionZone> findByExhibitionIdOrderByOrderIdAsc(UUID exhibitionId);
}
