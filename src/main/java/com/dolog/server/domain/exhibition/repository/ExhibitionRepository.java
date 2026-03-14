package com.dolog.server.domain.exhibition.repository;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExhibitionRepository extends JpaRepository<Exhibition, UUID> {
}
