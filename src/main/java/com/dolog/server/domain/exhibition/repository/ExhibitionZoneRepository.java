package com.dolog.server.domain.exhibition.repository;

import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExhibitionZoneRepository extends JpaRepository<ExhibitionZone, UUID> {

    List<ExhibitionZone> findByExhibitionId(UUID exhibitionId);

    boolean existsByExhibitionId(UUID exhibitionId);

    Optional<ExhibitionZone> findByIdAndExhibitionId(UUID id, UUID exhibitionId);


    // 존 조회 (orderId 기준 오름차순)
    List<ExhibitionZone> findByExhibitionIdOrderByOrderIdAsc(UUID exhibitionId);

    // 구역 생성 시 orderId를 안 보내면 기존 최대값 다음으로 자동 지정하기 위한 조회
    @Query("SELECT MAX(z.orderId) FROM ExhibitionZone z WHERE z.exhibition.id = :exhibitionId")
    Optional<Integer> findMaxOrderIdByExhibitionId(@Param("exhibitionId") UUID exhibitionId);
}
