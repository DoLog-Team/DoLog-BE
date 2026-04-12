package com.dolog.server.domain.bts.repository;

import com.dolog.server.domain.bts.entity.Bts;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface BtsRepository extends JpaRepository<Bts, UUID> {
    // 특정 전시의 BTS 목록을 최신순으로 가져올 때 유용합니다.
    List<Bts> findByExhibitionIdOrderByCreatedAtDesc(UUID exhibitionId);
}