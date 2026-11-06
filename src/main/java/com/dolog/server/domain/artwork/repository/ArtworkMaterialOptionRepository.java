package com.dolog.server.domain.artwork.repository;

import com.dolog.server.domain.artwork.entity.ArtworkMaterialOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ArtworkMaterialOptionRepository extends JpaRepository<ArtworkMaterialOption, UUID> {

    List<ArtworkMaterialOption> findAllByOrderByOrderIndexAsc();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);
}
