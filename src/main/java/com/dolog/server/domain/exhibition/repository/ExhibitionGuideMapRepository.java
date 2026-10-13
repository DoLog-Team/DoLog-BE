package com.dolog.server.domain.exhibition.repository;

import com.dolog.server.domain.exhibition.entity.ExhibitionGuideMap;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ExhibitionGuideMapRepository extends JpaRepository<ExhibitionGuideMap, UUID> {
    List<ExhibitionGuideMap> findByExhibitionId(UUID exhibitionId);

    // 등록 순서(먼저 올린 이미지가 먼저) 고정 — id가 랜덤 UUID라 정렬이 없으면 순서가 뒤섞임
    List<ExhibitionGuideMap> findByExhibitionIdOrderByCreatedAtAsc(UUID exhibitionId);

    List<ExhibitionGuideMap> findByZoneId(UUID zoneId);
}
