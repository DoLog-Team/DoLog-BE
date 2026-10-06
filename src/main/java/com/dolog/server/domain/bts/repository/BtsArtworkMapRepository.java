package com.dolog.server.domain.bts.repository;

import com.dolog.server.domain.bts.entity.BtsArtworkMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BtsArtworkMapRepository extends JpaRepository<BtsArtworkMap, Long> {
    // 필요 시 특정 BTS에 매핑된 작품들을 삭제하거나 조회하는 메서드 추가 가능
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update BtsArtworkMap m set m.deletedAt = :at, m.updatedAt = :at where m.artwork.id = :id and m.deletedAt is null")
    void hideByArtworkId(@org.springframework.data.repository.query.Param("id") java.util.UUID id,
                        @org.springframework.data.repository.query.Param("at") java.time.LocalDateTime at);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update BtsArtworkMap m set m.deletedAt = :at, m.updatedAt = :at where m.bts.id = :id and m.deletedAt is null")
    void hideByBtsId(@org.springframework.data.repository.query.Param("id") java.util.UUID id,
                        @org.springframework.data.repository.query.Param("at") java.time.LocalDateTime at);


    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("update BtsArtworkMap e set e.deletedAt = :at, e.updatedAt = :at where e.artwork.id in :ids and e.deletedAt is null")
    void hideByArtworkIds(@org.springframework.data.repository.query.Param("ids") java.util.Collection<java.util.UUID> ids,
            @org.springframework.data.repository.query.Param("at") java.time.LocalDateTime at);
}