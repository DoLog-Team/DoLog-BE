package com.dolog.server.domain.artwork.repository;

import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArtworkArtistMapRepository extends JpaRepository<ArtworkArtistMap, Long> {

    @Query("SELECT aam FROM ArtworkArtistMap aam " +
            "JOIN FETCH aam.artist " +
            "WHERE aam.artwork.id IN :artworkIds")
    List<ArtworkArtistMap> findByArtworkIdIn(@Param("artworkIds") List<UUID> artworkIds);

    // 2. 추가: 중복 등록 방지 (작품에 동일 작가가 이미 있는지 확인)
    boolean existsByArtworkIdAndArtistId(UUID artworkId, UUID artistId);

    // 3. 추가: 수정/삭제 시 해당 작품의 매핑이 맞는지 검증하며 조회
    Optional<ArtworkArtistMap> findByArtworkIdAndArtistId(UUID artworkId, UUID artistId);
}
