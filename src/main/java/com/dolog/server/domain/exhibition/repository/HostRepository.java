package com.dolog.server.domain.exhibition.repository;

import com.dolog.server.domain.exhibition.entity.Host;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HostRepository extends JpaRepository<Host, UUID> {
    Optional<Host> findByExhibitionId(UUID exhibitionId);
}
