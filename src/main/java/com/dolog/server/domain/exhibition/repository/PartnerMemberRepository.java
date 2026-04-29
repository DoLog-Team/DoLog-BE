package com.dolog.server.domain.exhibition.repository;

import com.dolog.server.domain.exhibition.entity.PartnerMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartnerMemberRepository extends JpaRepository<PartnerMember, UUID> {

    List<PartnerMember> findByPartnerId(UUID partnerId);

    List<PartnerMember> findByPartnerIdIn(List<UUID> partnerIds);

    List<PartnerMember> findByPartnerIdInOrderByNameAsc(List<UUID> partnerIds);

    void deleteByPartnerId(UUID partnerId);
}
