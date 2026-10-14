package com.dolog.server.domain.exhibition.repository;

import com.dolog.server.domain.exhibition.entity.ExhibitionGuideMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface ExhibitionGuideMapRepository extends JpaRepository<ExhibitionGuideMap, UUID> {
    List<ExhibitionGuideMap> findByExhibitionId(UUID exhibitionId);

    // 노출 순서(order_index) 우선, 같은 번호면 등록 순 — id가 랜덤 UUID라 정렬이 없으면 순서가 뒤섞임
    List<ExhibitionGuideMap> findByExhibitionIdOrderByOrderIndexAscCreatedAtAsc(UUID exhibitionId);

    // 등록 시 맨 뒤 번호(마지막 번호 + 1)를 부여하기 위한 현재 최대 번호
    @Query("select coalesce(max(g.orderIndex), 0) from ExhibitionGuideMap g where g.exhibition.id = :exhibitionId")
    int findMaxOrderIndexByExhibitionId(@Param("exhibitionId") UUID exhibitionId);

    List<ExhibitionGuideMap> findByZoneId(UUID zoneId);
}
