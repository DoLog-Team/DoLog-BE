package com.dolog.server.domain.exhibition.repository;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExhibitionDetailRepository extends JpaRepository<ExhibitionDetail, UUID> {

    // 1. 엔티티 객체로 상세 정보 찾기 (Service에서 profile.getExhibition() 넘길 때 사용)
    Optional<ExhibitionDetail> findByExhibition(Exhibition exhibition);

    // 2. ID값으로 상세 정보 찾기
    Optional<ExhibitionDetail> findByExhibitionId(UUID exhibitionId);

    // 3. 여러 개의 ID값으로 한꺼번에 상세 정보 찾기 (목록 조회 최적화용)
    List<ExhibitionDetail> findByExhibitionIdIn(List<UUID> exhibitionIds);

    @Query("SELECT ed FROM ExhibitionDetail ed JOIN FETCH ed.exhibition e " +
            "WHERE (:isPublic IS NULL OR e.isPublic = :isPublic) " +
            "AND (:univName IS NULL OR e.univName = :univName) " +
            "AND (:search IS NULL OR ed.title LIKE CONCAT('%', :search, '%'))")
    List<ExhibitionDetail> findExhibitions(
            @Param("isPublic") Boolean isPublic,
            @Param("univName") String univName,
            @Param("search") String search);

    @Query("SELECT ed FROM ExhibitionDetail ed JOIN FETCH ed.exhibition e " +
            "WHERE e.isPublic = true " +
            "ORDER BY e.createdAt DESC")
    List<ExhibitionDetail> findTop3PublicExhibitions(Pageable pageable);
}