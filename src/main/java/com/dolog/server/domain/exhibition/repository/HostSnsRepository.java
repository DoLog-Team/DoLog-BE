package com.dolog.server.domain.exhibition.repository;

import com.dolog.server.domain.exhibition.entity.HostSns;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HostSnsRepository extends JpaRepository<HostSns, UUID> {
    List<HostSns> findByHostId(UUID hostId);
}
