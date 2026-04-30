package com.dolog.server.domain.artwork.repository;

import com.dolog.server.domain.artwork.entity.Artwork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArtworkRepository extends JpaRepository<Artwork, UUID>, JpaSpecificationExecutor<Artwork> {
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



    @Query("SELECT DISTINCT a FROM Artwork a " +
            "WHERE a.id = :artworkId AND a.exhibition.id = :exhibitionId")
    Optional<Artwork> findDetailById(@Param("exhibitionId") UUID exhibitionId, @Param("artworkId") UUID artworkId);


    @Query("SELECT DISTINCT a FROM Artwork a " +
            "LEFT JOIN FETCH a.artworkArtistMaps am " +
            "LEFT JOIN FETCH am.artist art " +
            "WHERE (a.title LIKE %:search% OR art.nameKo LIKE %:search%) " +
            "AND a.exhibition.id = :exhibitionId")
    List<Artwork> findArtworksBySearch(UUID exhibitionId, String search);

    // 1. 동일 카테고리 내 인근 작품 (orderIndex가 같을 경우 id 순 정렬)
    // 현재 작품보다 순서가 뒤인 것 2개를 먼저 찾고, 부족하면 앞의 것을 가져오는 로직은 서비스에서 처리하거나
    // 혹은 단순히 '현재 작품 제외' 후 순서가 가장 가까운 2개를 가져옵니다.
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
            org.springframework.data.domain.Pageable pageable);

    // 2. 전체 작품 중 가나다순 상위 2개
    @Query("SELECT a FROM Artwork a " +
            "WHERE a.exhibition.id = :exhibitionId " +
            "ORDER BY a.title ASC, a.id ASC")
    List<Artwork> findTopAlphabetical(
            @Param("exhibitionId") UUID exhibitionId,
            org.springframework.data.domain.Pageable pageable);
}
