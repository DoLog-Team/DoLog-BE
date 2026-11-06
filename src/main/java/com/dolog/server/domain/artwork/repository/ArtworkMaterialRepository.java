package com.dolog.server.domain.artwork.repository;

import com.dolog.server.domain.artwork.entity.ArtworkMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ArtworkMaterialRepository extends JpaRepository<ArtworkMaterial, UUID> {

    // 작가 등록 폼 "재료 및 기법" 자동완성용. 본인이 지금까지 등록한 작품들에 실제로 입력했던 재료 이름 목록(중복 제거).
    @Query("""
            SELECT DISTINCT m.name
            FROM ArtworkMaterial m
            JOIN m.artwork a
            JOIN a.artworkArtistMaps aam
            WHERE aam.artist.id = :artistId
            ORDER BY m.name
            """)
    List<String> findDistinctNamesByArtistId(@Param("artistId") UUID artistId);
}
