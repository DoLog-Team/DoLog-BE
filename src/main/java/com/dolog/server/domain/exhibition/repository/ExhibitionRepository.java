package com.dolog.server.domain.exhibition.repository;

import com.dolog.server.domain.exhibition.entity.Exhibition;
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
            "AND (:search IS NULL OR d.title LIKE CONCAT('%', :search, '%'))")
    List<Exhibition> findExhibitions(
            @Param("isPublic") Boolean isPublic,
            @Param("univName") String univName,
            @Param("search") String search);

    // 최신순 메인 전시
    @Query("""
            SELECT e FROM Exhibition e
            JOIN e.exhibitionDetail d
            WHERE e.isPublic = true
              AND d.startDate <= :today
              AND d.endDate >= :today
            ORDER BY d.startDate DESC
        """)
    List<Exhibition> findOngoingExhibitions(
            @Param("today") LocalDate today,
            Pageable pageable
    );

}
