package com.dolog.server.domain.artwork.repository;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArtworkRepository extends JpaRepository<Artwork, UUID>, JpaSpecificationExecutor<Artwork> {

    // 전시 URL 노출 조건: 공개 + 작품 숨김 아님 + 구역 숨김 아님. 쿼리마다 `LEFT JOIN a.exhibitionZone vz` 가 필요하다.
    String EXHIBITION_VISIBLE =
            " AND a.status = com.dolog.server.domain.artwork.entity.enums.ArtworkStatus.PUBLISHED" +
            " AND a.hiddenAt IS NULL AND (vz.id IS NULL OR vz.hidden = false) ";

    // 두록 URL 은 공개만 본다. exhibitionView 가 true 면 전시 URL 조건까지 본다.
    String VISIBLE_BY_VIEW =
            " AND a.status = com.dolog.server.domain.artwork.entity.enums.ArtworkStatus.PUBLISHED" +
            " AND (:exhibitionView = false OR (a.hiddenAt IS NULL AND (vz.id IS NULL OR vz.hidden = false))) ";

    // 삭제될 zone에 속한 작품들의 zone을 해제한다 (작품은 유지)
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Artwork a SET a.exhibitionZone = null WHERE a.exhibitionZone IN :zones")
    void clearZone(@Param("zones") Collection<ExhibitionZone> zones);

    @EntityGraph(attributePaths = {"artworkArtistMaps", "artworkArtistMaps.artist"})
    @Query("SELECT DISTINCT aam.artwork FROM ArtworkArtistMap aam " +
            "WHERE aam.artwork.exhibition.id = :exhibitionId " +
            "AND aam.artist.id IN :artistIds")
    List<Artwork> findSubmittedArtworksByExhibitionIdAndArtistIdIn(
            @Param("exhibitionId") UUID exhibitionId,
            @Param("artistIds") List<UUID> artistIds
    );

    // ==============================================================================
    // 메인 화면 랜덤 페이징 쿼리
    // ==============================================================================
    long count();
    Page<Artwork> findAll(Pageable pageable);


    /**
     * 작품 목록 조회 (전시 + zone + 카테고리 필터)
     */
    @Query("SELECT DISTINCT a FROM Artwork a " +
            "LEFT JOIN FETCH a.exhibitionZone vz " +
            "LEFT JOIN FETCH a.artworkArtistMaps aam " +
            "LEFT JOIN FETCH aam.artist " +
            "LEFT JOIN FETCH aam.artistProfile " +
            "WHERE a.exhibition.id = :exhibitionId " + EXHIBITION_VISIBLE +
            "AND (:zone IS NULL OR vz.name = :zone) " +
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
            "LEFT JOIN FETCH a.exhibitionZone vz " +
            "LEFT JOIN FETCH a.artworkArtistMaps am " +
            "LEFT JOIN FETCH am.artist art " +
            "LEFT JOIN FETCH am.artistProfile " +
            "WHERE (a.title LIKE %:search% OR art.nameKo LIKE %:search%) " +
            "AND a.exhibition.id = :exhibitionId" + EXHIBITION_VISIBLE)
    List<Artwork> findArtworksBySearch(@Param("exhibitionId") UUID exhibitionId,
                                       @Param("search") String search);



    // ==============================================================================
    // 3. 동일 카테고리 내 작가 기준 조회
    // ==============================================================================
    @Query(value = "SELECT DISTINCT a.*, " +
            "CASE WHEN am.artist_id IN (SELECT cm.artist_id FROM artwork_artist_maps cm WHERE cm.deleted_at IS NULL AND cm.artwork_id = :artworkId) THEN 2 " +
            "     WHEN a.category = :category THEN 1 " +
            "     ELSE 0 END as score " +
            "FROM artworks a " +
            "LEFT JOIN artwork_artist_maps am ON a.id = am.artwork_id AND am.deleted_at IS NULL " +
            "LEFT JOIN exhibition_zones vz ON vz.id = a.zone_id " +
            "WHERE a.deleted_at IS NULL AND a.exhibition_id = :exhibitionId " +
            "AND a.status = 'PUBLISHED' " +
            "AND (:exhibitionView = false OR (a.hidden_at IS NULL AND (vz.id IS NULL OR vz.is_hidden = false))) " +
            "AND a.id != :artworkId " +
            "AND (am.artist_id IN (SELECT cm.artist_id FROM artwork_artist_maps cm WHERE cm.deleted_at IS NULL AND cm.artwork_id = :artworkId) OR a.category = :category) " +
            "ORDER BY score DESC, a.id DESC",
            nativeQuery = true)
    List<Artwork> findRelatedArtworks(
            @Param("exhibitionId") UUID exhibitionId,
            @Param("artworkId") UUID artworkId,
            @Param("category") String category,
            @Param("exhibitionView") boolean exhibitionView,
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
            "LEFT JOIN a.exhibitionZone vz " +
            "WHERE a.exhibition.id = :exhibitionId" + VISIBLE_BY_VIEW +
            "AND vz.id = :zoneId " +
            "AND a.orderIndex < :orderIndex " +
            "ORDER BY a.orderIndex DESC")
    List<Artwork> findPrevInSameZone(
            @Param("exhibitionId") UUID exhibitionId,
            @Param("zoneId") UUID zoneId,
            @Param("orderIndex") Integer orderIndex,
            @Param("exhibitionView") boolean exhibitionView,
            Pageable pageable
    );

    // 2. (위 쿼리 결과가 없을 때) 이전 존
    @Query("SELECT a FROM Artwork a " +
            "LEFT JOIN a.exhibitionZone vz " +
            "WHERE a.exhibition.id = :exhibitionId" + VISIBLE_BY_VIEW +
            "AND vz.orderId < :zoneOrderId " +
            "ORDER BY vz.orderId DESC, a.orderIndex DESC")
    List<Artwork> findPrevInPrevZone(
            @Param("exhibitionId") UUID exhibitionId,
            @Param("zoneOrderId") Integer zoneOrderId,
            @Param("exhibitionView") boolean exhibitionView,
            Pageable pageable
    );

    /**
     * 4-2. '다음' 동선에 있는 작품 조회
     */
    // 1. 같은 존
    @Query("SELECT a FROM Artwork a " +
            "LEFT JOIN a.exhibitionZone vz " +
            "WHERE a.exhibition.id = :exhibitionId" + VISIBLE_BY_VIEW +
            "AND vz.id = :zoneId " +
            "AND a.orderIndex > :orderIndex " +
            "ORDER BY a.orderIndex ASC")
    List<Artwork> findNextInSameZone(
            @Param("exhibitionId") UUID exhibitionId,
            @Param("zoneId") UUID zoneId,
            @Param("orderIndex") Integer orderIndex,
            @Param("exhibitionView") boolean exhibitionView,
            Pageable pageable
    );

    // 2. (위 쿼리 결과가 없을 때) 다음 존
    @Query("SELECT a FROM Artwork a " +
            "LEFT JOIN a.exhibitionZone vz " +
            "WHERE a.exhibition.id = :exhibitionId" + VISIBLE_BY_VIEW +
            "AND vz.orderId > :zoneOrderId " +
            "ORDER BY vz.orderId ASC, a.orderIndex ASC")
    List<Artwork> findNextInNextZone(
            @Param("exhibitionId") UUID exhibitionId,
            @Param("zoneOrderId") Integer zoneOrderId,
            @Param("exhibitionView") boolean exhibitionView,
            Pageable pageable
    );


    /**
     * exhibition + zone 기준 정렬
     */
    List<Artwork> findByExhibitionIdAndExhibitionZoneIdOrderByOrderIndexAsc(
            UUID exhibitionId,
            UUID zoneId
    );


    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("update Artwork e set e.deletedAt = :at, e.updatedAt = :at where e.id in :ids and e.deletedAt is null")
    void hideByIds(@org.springframework.data.repository.query.Param("ids") java.util.Collection<java.util.UUID> ids,
            @org.springframework.data.repository.query.Param("at") java.time.LocalDateTime at);

    List<Artwork> findByExhibitionIdAndStatus(UUID exhibitionId, ArtworkStatus status);

    // 동시 요청에도 값이 빠지지 않게 DB 에서 더한다. updated_at 은 바꾸지 않는다.
    @Modifying
    @Query("UPDATE Artwork a SET a.viewCount = a.viewCount + 1 WHERE a.id = :artworkId")
    void increaseViewCount(@Param("artworkId") UUID artworkId);

    // 이 방문자의 첫 조회면 기록하고 1, 이미 있으면 0. UNIQUE(artwork_id, visitor_id) 라 동시에 와도 한 번만 1.
    // ON DUPLICATE KEY UPDATE 는 MySQL 드라이버 기본값(CLIENT_FOUND_ROWS)에서 중복도 1 을 돌려줘 구분이 안 된다.
    // IGNORE 가 삼킬 수 있는 다른 오류(FK, 길이)는 호출 전에 작품 존재와 visitorId 형식(64자)을 확인해 막는다.
    @Modifying
    @Query(value = "INSERT IGNORE INTO artwork_view_logs (artwork_id, visitor_id, created_at) " +
            "VALUES (:artworkId, :visitorId, NOW(6))", nativeQuery = true)
    int insertViewLogIfAbsent(@Param("artworkId") UUID artworkId, @Param("visitorId") String visitorId);
}
