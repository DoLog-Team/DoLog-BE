package com.dolog.server.domain.exhibition.repository;

import com.dolog.server.domain.exhibition.entity.Partner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartnerRepository extends JpaRepository<Partner, UUID> {

    List<Partner> findByExhibitionIdOrderByOrderAscCreatedAtAsc(UUID exhibitionId);
}
