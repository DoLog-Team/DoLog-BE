package com.dolog.server.domain.exhibition.repository;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExhibitionRepository extends JpaRepository<Exhibition, UUID> {

    @Query("SELECT e FROM Exhibition e LEFT JOIN FETCH e.exhibitionDetail d " +
            "WHERE (:isPublic IS NULL OR e.isPublic = :isPublic) " +
            "AND (:univName IS NULL OR e.univName = :univName) " +
            "AND (:exhibitionType IS NULL OR e.exhibitionType = :exhibitionType) " +
            "AND (:search IS NULL OR d.title LIKE CONCAT('%', :search, '%'))")
    List<Exhibition> findExhibitions(
            @Param("isPublic") Boolean isPublic,
            @Param("univName") String univName,
            @Param("exhibitionType") ExhibitionType exhibitionType,
            @Param("search") String search);

    // 진행 + 진행 예정
    @Query("""
            SELECT e FROM Exhibition e
            JOIN FETCH e.exhibitionDetail d
            WHERE e.isPublic = true
              AND d.endDate >= :today
        """)
    List<Exhibition> findDefaultExhibitions(
            @Param("today") LocalDate today
    );

    Optional<Exhibition> findBySlug(String slug);
    boolean existsBySlug(String slug);

    // 진행 중 전시 (최신순)
    @Query("""
            SELECT e FROM Exhibition e
            JOIN FETCH e.exhibitionDetail d
            WHERE e.isPublic = true
              AND d.startDate <= :today
              AND d.endDate >= :today
            ORDER BY d.startDate DESC
        """)
    List<Exhibition> findLatestExhibitions(@Param("today") LocalDate today);

}
