package com.dolog.server.domain.bts.repository;

import com.dolog.server.domain.bts.entity.Bts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BtsRepository extends JpaRepository<Bts, UUID> {
    // 특정 전시의 BTS 목록을 최신순으로 가져올 때 유용합니다.
    List<Bts> findByExhibitionIdOrderByCreatedAtDesc(UUID exhibitionId);

    @Query("SELECT DISTINCT b FROM Bts b " +
            "LEFT JOIN FETCH b.artist " +
            "LEFT JOIN FETCH b.artworkMaps m " +
            "LEFT JOIN FETCH m.artwork " +
            "WHERE b.exhibition.id = :exhibitionId")
    List<Bts> findAllByExhibitionId(@Param("exhibitionId") UUID exhibitionId);
}