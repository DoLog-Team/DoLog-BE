package com.dolog.server.domain.exhibition.repository;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("SELECT e FROM Exhibition e LEFT JOIN FETCH e.exhibitionDetail d " +
            "WHERE e.isPublic = true " +
            "ORDER BY e.createdAt DESC")
    List<Exhibition> findTop3PublicExhibitions(Pageable pageable);

}
