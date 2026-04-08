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

    // 전시회 엔티티를 넘기면 그 전시의 상세 정보(제목, 날짜 등)를 찾아옵니다.
    Optional<ExhibitionDetail> findByExhibition(Exhibition exhibition);

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
