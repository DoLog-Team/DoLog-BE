package com.dolog.server.domain.artwork.repository;

import com.dolog.server.domain.artwork.entity.ArtworkCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ArtworkCategoryRepository extends JpaRepository<ArtworkCategory, UUID> {

    List<ArtworkCategory> findAllByOrderByOrderIndexAsc();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);
}
