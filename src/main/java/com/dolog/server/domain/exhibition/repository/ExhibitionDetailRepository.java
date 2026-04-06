package com.dolog.server.domain.exhibition.repository;

import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExhibitionDetailRepository extends JpaRepository<ExhibitionDetail, UUID> {

    Optional<ExhibitionDetail> findByExhibitionId(UUID exhibitionId);

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
