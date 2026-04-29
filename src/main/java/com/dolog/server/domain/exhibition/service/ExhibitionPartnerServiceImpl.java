package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.Partner;
import com.dolog.server.domain.exhibition.entity.PartnerMember;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.repository.PartnerMemberRepository;
import com.dolog.server.domain.exhibition.repository.PartnerRepository;
import com.dolog.server.domain.exhibition.web.dto.request.partner.PartnerMemberCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.partner.PartnerMemberUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.partner.PartnerListResponse;
import com.dolog.server.domain.exhibition.web.dto.request.partner.PartnerPartCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.partner.PartnerPartUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.partner.PartnerMemberResponse;
import com.dolog.server.domain.exhibition.web.dto.response.partner.PartnerPartResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ExhibitionPartnerServiceImpl implements ExhibitionPartnerService {

    private final ExhibitionRepository exhibitionRepository;
    private final PartnerRepository partnerRepository;
    private final PartnerMemberRepository partnerMemberRepository;

    @Override
    @Transactional(readOnly = true)
    public PartnerListResponse getPartners(UUID exhibitionId, String sort) {
        exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        List<Partner> partners = partnerRepository.findByExhibitionIdOrderByOrderAscCreatedAtAsc(exhibitionId);
        List<UUID> partnerIds = partners.stream().map(Partner::getId).collect(java.util.stream.Collectors.toList());

        List<PartnerMember> allMembers = "name".equals(sort)
                ? partnerMemberRepository.findByPartnerIdInOrderByNameAsc(partnerIds)
                : partnerMemberRepository.findByPartnerIdIn(partnerIds);

        return PartnerListResponse.from(partners, allMembers);
    }

    @Override
    public PartnerPartResponse createPart(UUID exhibitionId, PartnerPartCreateRequest request) {
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        Partner partner = Partner.builder()
                .exhibition(exhibition)
                .name(request.getPartName())
                .order(request.getOrder())
                .build();

        partnerRepository.save(partner);

        return PartnerPartResponse.from(partner);
    }

    @Override
    public PartnerPartResponse updatePart(UUID partId, PartnerPartUpdateRequest request) {
        Partner partner = partnerRepository.findById(partId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.PARTNER_NOT_FOUND));

        partner.update(request.getPartName(), request.getOrder());

        return PartnerPartResponse.from(partner);
    }

    @Override
    public void deletePart(UUID partId) {
        Partner partner = partnerRepository.findById(partId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.PARTNER_NOT_FOUND));

        partnerMemberRepository.deleteByPartnerId(partId);
        partnerRepository.delete(partner);
    }

    @Override
    public PartnerMemberResponse createMember(UUID partId, PartnerMemberCreateRequest request) {
        Partner partner = partnerRepository.findById(partId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.PARTNER_NOT_FOUND));

        PartnerMember member = PartnerMember.builder()
                .partner(partner)
                .name(request.getMemberName())
                .nameEn(request.getMemberNameEn())
                .imageUrl(request.getMemberImageUrl())
                .email(request.getMemberEmail())
                .build();

        partnerMemberRepository.save(member);

        return PartnerMemberResponse.from(member);
    }

    @Override
    public PartnerMemberResponse updateMember(UUID memberId, PartnerMemberUpdateRequest request) {
        PartnerMember member = partnerMemberRepository.findById(memberId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.PARTNER_MEMBER_NOT_FOUND));

        member.update(request.getMemberName(), request.getMemberNameEn(), request.getMemberEmail(), request.getMemberImageUrl());

        return PartnerMemberResponse.from(member);
    }

    @Override
    public void deleteMember(UUID memberId) {
        PartnerMember member = partnerMemberRepository.findById(memberId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.PARTNER_MEMBER_NOT_FOUND));

        partnerMemberRepository.delete(member);
    }
}
