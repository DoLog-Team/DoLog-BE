package com.dolog.server.domain.bts.repository;

import com.dolog.server.domain.bts.entity.BtsArtworkMap;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BtsArtworkMapRepository extends JpaRepository<BtsArtworkMap, Long> {
    // 필요 시 특정 BTS에 매핑된 작품들을 삭제하거나 조회하는 메서드 추가 가능
}