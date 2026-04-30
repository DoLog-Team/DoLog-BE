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

    /**
     * 동일 카테고리 내 인근 작품
     */
    @Query("SELECT a FROM Artwork a " +
            "WHERE a.exhibition.id = :exhibitionId " +
            "AND a.category = :category " +
            "AND a.id != :artworkId " +
            "ORDER BY ABS(a.orderIndex - :currentOrder) ASC, a.id ASC")
    List<Artwork> findRelatedByCategory(
            @Param("exhibitionId") UUID exhibitionId,
            @Param("category") String category,
            @Param("artworkId") UUID artworkId,
            @Param("currentOrder") Integer currentOrder,
            Pageable pageable);

    /**
     * 가나다순 상위 작품
     */
    @Query("SELECT a FROM Artwork a " +
            "WHERE a.exhibition.id = :exhibitionId " +
            "ORDER BY a.title ASC, a.id ASC")
    List<Artwork> findTopAlphabetical(
            @Param("exhibitionId") UUID exhibitionId,
            Pageable pageable);

    /**
     * exhibition + zone 기준 정렬
     */
    List<Artwork> findByExhibitionIdAndExhibitionZoneIdOrderByOrderIndexAsc(
            UUID exhibitionId,
            UUID zoneId
    );
}


