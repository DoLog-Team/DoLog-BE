package com.dolog.server.domain.exhibition.repository;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExhibitionDetailRepository extends JpaRepository<ExhibitionDetail, UUID> {

    // 1. 엔티티 객체로 상세 정보 찾기 (Service에서 profile.getExhibition() 넘길 때 사용)
    Optional<ExhibitionDetail> findByExhibition(Exhibition exhibition);

    // 2. ID값으로 상세 정보 찾기
    Optional<ExhibitionDetail> findByExhibitionId(UUID exhibitionId);

    // 3. 여러 개의 ID값으로 한꺼번에 상세 정보 찾기 (목록 조회 최적화용)
    List<ExhibitionDetail> findByExhibitionIdIn(List<UUID> exhibitionIds);

}