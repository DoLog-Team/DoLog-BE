package com.dolog.server.domain.exhibition.repository;

import com.dolog.server.domain.exhibition.entity.PartnerMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartnerMemberRepository extends JpaRepository<PartnerMember, UUID> {

    List<PartnerMember> findByPartnerId(UUID partnerId);

    List<PartnerMember> findByPartnerIdIn(List<UUID> partnerIds);

    @Query("SELECT m FROM PartnerMember m WHERE m.partner.id IN :partnerIds ORDER BY m.order ASC, m.createdAt ASC")
    List<PartnerMember> findByPartnerIdInOrderByOrderAscCreatedAtAsc(@Param("partnerIds") List<UUID> partnerIds);

    List<PartnerMember> findByPartnerIdInOrderByNameAsc(List<UUID> partnerIds);

    @Query("SELECT MAX(m.order) FROM PartnerMember m WHERE m.partner.id = :partnerId")
    Optional<Integer> findMaxOrderByPartnerId(@Param("partnerId") UUID partnerId);

    void deleteByPartnerId(UUID partnerId);
}
