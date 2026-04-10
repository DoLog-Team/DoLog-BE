package com.dolog.server.domain.exhibition.repository;

import com.dolog.server.domain.exhibition.entity.ExhibitionCustomTheme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExhibitionCustomThemeRepository extends JpaRepository<ExhibitionCustomTheme, UUID> {

    Optional<ExhibitionCustomTheme> findByExhibitionId(UUID exhibitionId);
}
