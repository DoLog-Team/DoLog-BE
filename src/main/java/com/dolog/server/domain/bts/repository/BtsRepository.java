package com.dolog.server.domain.bts.repository;

import com.dolog.server.domain.bts.entity.Bts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BtsRepository extends JpaRepository<Bts, UUID> {

    // 1. 최신순 목록 조회
    List<Bts> findByExhibitionIdOrderByCreatedAtDesc(UUID exhibitionId);

    // 2. 패치 조인 쿼리 (b.artist -> b.artistProfile로 수정)
    @Query("SELECT DISTINCT b FROM Bts b " +
            "LEFT JOIN FETCH b.artistProfile " + // 필드명 수정됨
            "LEFT JOIN FETCH b.artworkMaps m " +
            "LEFT JOIN FETCH m.artwork " +
            "WHERE b.exhibition.id = :exhibitionId")
    List<Bts> findAllByExhibitionId(@Param("exhibitionId") UUID exhibitionId);

    // 3. 특정 작가의 BTS 목록 조회
    List<Bts> findAllByArtistProfileIdAndExhibitionId(UUID artistProfileId, UUID exhibitionId);

    // 4. 추천
    // 1순위: 같은 전시회 내 특정 작가의 최신 BTS (현재 본인 제외)
    List<Bts> findTop3ByExhibitionIdAndArtistProfileIdAndIdNotOrderByCreatedAtDesc(
            UUID exhibitionId, UUID artistProfileId, UUID btsId);

    List<Bts> findTop3ByExhibitionIdAndIdNotOrderByCreatedAtDesc(UUID exhibitionId, UUID btsId);

    @Query("SELECT b FROM Bts b JOIN b.artworkMaps m WHERE m.artwork.id = :artworkId")
    List<Bts> findAllByArtworkId(@Param("artworkId") UUID artworkId);
}