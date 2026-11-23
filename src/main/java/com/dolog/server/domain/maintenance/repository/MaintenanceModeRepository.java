package com.dolog.server.domain.maintenance.repository;

import com.dolog.server.domain.maintenance.entity.MaintenanceMode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceModeRepository extends JpaRepository<MaintenanceMode, Long> {
}
