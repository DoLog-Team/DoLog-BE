package com.dolog.server.domain.artwork.repository;

import com.dolog.server.domain.artwork.entity.ExhibitionFieldSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExhibitionFieldSettingsRepository extends JpaRepository<ExhibitionFieldSettings, UUID> {

    Optional<ExhibitionFieldSettings> findByExhibitionId(UUID exhibitionId);
}
