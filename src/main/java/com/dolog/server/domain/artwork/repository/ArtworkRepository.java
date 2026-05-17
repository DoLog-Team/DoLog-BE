package com.dolog.server.domain.artwork.repository;

import com.dolog.server.domain.artwork.entity.Artwork;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArtworkRepository extends JpaRepository<Artwork, UUID>, JpaSpecificationExecutor<Artwork> {
    /**
     * 작품 목록 조회 (전시 + zone + 카테고리 필터)
     */
    @Query("SELECT DISTINCT a FROM Artwork a " +
            "LEFT JOIN FETCH a.exhibitionZone " +
            "LEFT JOIN FETCH a.artworkArtistMaps aam " +
            "LEFT JOIN FETCH aam.artist " +
            "WHERE a.exhibition.id = :exhibitionId " +
            "AND (:zone IS NULL OR a.exhibitionZone.name = :zone) " +
            "AND (:category IS NULL OR a.category = :category) " +
            "ORDER BY a.orderIndex ASC")
    List<Artwork> findArtworksForList(@Param("exhibitionId") UUID exhibitionId,
                                      @Param("zone") String zone,
                                      @Param("category") String category);



    /**
     * 작품 상세 조회
     */
    @Query("SELECT DISTINCT a FROM Artwork a " +
            "WHERE a.id = :artworkId AND a.exhibition.id = :exhibitionId")
    Optional<Artwork> findDetailById(@Param("exhibitionId") UUID exhibitionId,
                                     @Param("artworkId") UUID artworkId);


    /**
     * 검색 (작품명 + 작가명)
     */
    @Query("SELECT DISTINCT a FROM Artwork a " +
            "LEFT JOIN FETCH a.artworkArtistMaps am " +
            "LEFT JOIN FETCH am.artist art " +
            "WHERE (a.title LIKE %:search% OR art.nameKo LIKE %:search%) " +
            "AND a.exhibition.id = :exhibitionId")
    List<Artwork> findArtworksBySearch(@Param("exhibitionId") UUID exhibitionId,
                                       @Param("search") String search);



    // ==============================================================================
    // 3. 동일 카테고리 내 작가 기준 조회
    // ==============================================================================
    @Query(value = "SELECT DISTINCT a.*, " +
            "CASE WHEN am.artist_id IN (SELECT cm.artist_id FROM artwork_artist_maps cm WHERE cm.artwork_id = :artworkId) THEN 2 " +
            "     WHEN a.category = :category THEN 1 " +
            "     ELSE 0 END as score " +
            "FROM artworks a " +
            "LEFT JOIN artwork_artist_maps am ON a.id = am.artwork_id " +
            "WHERE a.exhibition_id = :exhibitionId " +
            "AND a.id != :artworkId " +
            "AND (am.artist_id IN (SELECT cm.artist_id FROM artwork_artist_maps cm WHERE cm.artwork_id = :artworkId) OR a.category = :category) " +
            "ORDER BY score DESC, a.id DESC",
            nativeQuery = true)
    List<Artwork> findRelatedArtworks(
            @Param("exhibitionId") UUID exhibitionId,
            @Param("artworkId") UUID artworkId,
            @Param("category") String category,
            Pageable pageable
    );


    // ==============================================================================
    // 4. 전시 전체 동선(Zone 순서[orderId] -> Artwork 순서[orderIndex]) 기준 이전 / 다음 조회
    // ==============================================================================

    /**
     * 4-1. '이전' 동선에 있는 작품 조회
     * 조건 1: 같은 존 안에서 나보다 orderIndex가 작거나
     * 조건 2: 나보다 존의 순서(zone.orderId)가 작은 전체 존의 작품
     */

    // 1. 같은 존
    @Query("SELECT a FROM Artwork a " +
            "WHERE a.exhibition.id = :exhibitionId " +
            "AND a.exhibitionZone.id = :zoneId " +
            "AND a.orderIndex < :orderIndex " +
            "ORDER BY a.orderIndex DESC")
    List<Artwork> findPrevInSameZone(
            @Param("exhibitionId") UUID exhibitionId,
            @Param("zoneId") UUID zoneId,
            @Param("orderIndex") Integer orderIndex,
            Pageable pageable
    );

    // 2. (위 쿼리 결과가 없을 때) 이전 존
    @Query("SELECT a FROM Artwork a " +
            "WHERE a.exhibition.id = :exhibitionId " +
            "AND a.exhibitionZone.orderId < :zoneOrderId " +
            "ORDER BY a.exhibitionZone.orderId DESC, a.orderIndex DESC")
    List<Artwork> findPrevInPrevZone(
            @Param("exhibitionId") UUID exhibitionId,
            @Param("zoneOrderId") Integer zoneOrderId,
            Pageable pageable
    );

    /**
     * 4-2. '다음' 동선에 있는 작품 조회
     */
    // 1. 같은 존
    @Query("SELECT a FROM Artwork a " +
            "WHERE a.exhibition.id = :exhibitionId " +
            "AND a.exhibitionZone.id = :zoneId " +
            "AND a.orderIndex > :orderIndex " +
            "ORDER BY a.orderIndex ASC")
    List<Artwork> findNextInSameZone(
            @Param("exhibitionId") UUID exhibitionId,
            @Param("zoneId") UUID zoneId,
            @Param("orderIndex") Integer orderIndex,
            Pageable pageable
    );

    // 2. (위 쿼리 결과가 없을 때) 다음 존
    @Query("SELECT a FROM Artwork a " +
            "WHERE a.exhibition.id = :exhibitionId " +
            "AND a.exhibitionZone.orderId > :zoneOrderId " +
            "ORDER BY a.exhibitionZone.orderId ASC, a.orderIndex ASC")
    List<Artwork> findNextInNextZone(
            @Param("exhibitionId") UUID exhibitionId,
            @Param("zoneOrderId") Integer zoneOrderId,
            Pageable pageable
    );


    /**
     * exhibition + zone 기준 정렬
     */
    List<Artwork> findByExhibitionIdAndExhibitionZoneIdOrderByOrderIndexAsc(
            UUID exhibitionId,
            UUID zoneId
    );
}


