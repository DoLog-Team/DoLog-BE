package com.dolog.server.domain.exhibition.repository;

import com.dolog.server.domain.exhibition.entity.PartnerMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PartnerMemberRepository extends JpaRepository<PartnerMember, UUID> {
}
