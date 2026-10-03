package com.dolog.server.domain.plan.repository;

import com.dolog.server.domain.plan.entity.PlanTargetSize;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlanTargetSizeRepository extends JpaRepository<PlanTargetSize, UUID> {
}
