package com.dolog.server.domain.plan.repository;

import com.dolog.server.domain.plan.entity.PlanPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlanPriceRepository extends JpaRepository<PlanPrice, UUID> {
}
