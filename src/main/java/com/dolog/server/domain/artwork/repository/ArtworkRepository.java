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

    /**
     * 3-1. 동일 작가 다른 작품 조회
     */
    @Query("SELECT DISTINCT a FROM Artwork a " +
            "JOIN a.artworkArtistMaps map " +
            "WHERE a.exhibition.id = :exhibitionId " +
            "AND a.id != :artworkId " +
            "AND map.artist.id IN (" +
            "    SELECT currentMap.artist.id FROM ArtworkArtistMap currentMap WHERE currentMap.artwork.id = :artworkId" +
            ")")
    List<Artwork> findBySameArtists(
            @Param("exhibitionId") UUID exhibitionId,
            @Param("artworkId") UUID artworkId
    );

    /**
     * 3-2. 동일 카테고리 내에서 '현재 작품의 작가들'이 참여하지 않은 다른 작품들을 랜덤하게 조회
     */
    @Query("SELECT DISTINCT a FROM Artwork a " +
            "JOIN a.artworkArtistMaps map " +
            "WHERE a.exhibition.id = :exhibitionId " +
            "AND a.category = :category " +
            "AND map.artist.id NOT IN (" +
            "    SELECT currentMap.artist.id FROM ArtworkArtistMap currentMap WHERE currentMap.artwork.id = :artworkId" +
            ") " +
            "ORDER BY FUNCTION('RAND')")
    List<Artwork> findSameCategoryRandom(
            @Param("exhibitionId") UUID exhibitionId,
            @Param("category") String category,
            @Param("artworkId") UUID artworkId,
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
    @Query("SELECT a FROM Artwork a " +
            "JOIN a.exhibitionZone z " +
            "WHERE a.exhibition.id = :exhibitionId " +
            "AND (" +
            "  (z.id = :zoneId AND a.orderIndex < :currentOrderIndex) " +
            "  OR (z.orderId < :zoneOrderId)" +
            ") " +
            "ORDER BY z.orderId DESC, a.orderIndex DESC")
    List<Artwork> findGlobalPrevArtwork(
            @Param("exhibitionId") UUID exhibitionId,
            @Param("zoneId") UUID zoneId,
            @Param("zoneOrderId") Integer zoneOrderId,
            @Param("currentOrderIndex") Integer currentOrderIndex,
            Pageable pageable
    );

    /**
     * 4-2. '다음' 동선에 있는 작품 조회
     */
    @Query("SELECT a FROM Artwork a " +
            "JOIN a.exhibitionZone z " +
            "WHERE a.exhibition.id = :exhibitionId " +
            "AND (" +
            "  (z.id = :zoneId AND a.orderIndex > :currentOrderIndex) " +
            "  OR (z.orderId > :zoneOrderId)" +
            ") " +
            "ORDER BY z.orderId ASC, a.orderIndex ASC")
    List<Artwork> findGlobalNextArtwork(
            @Param("exhibitionId") UUID exhibitionId,
            @Param("zoneId") UUID zoneId,
            @Param("zoneOrderId") Integer zoneOrderId,
            @Param("currentOrderIndex") Integer currentOrderIndex,
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


