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
import com.dolog.server.domain.exhibition.web.dto.response.partner.PartnerMemberCreateResponse;
import com.dolog.server.domain.exhibition.web.dto.response.partner.PartnerMemberUpdateResponse;
import com.dolog.server.domain.exhibition.web.dto.response.partner.PartnerPartCreateResponse;
import com.dolog.server.domain.exhibition.web.dto.response.partner.PartnerPartUpdateResponse;
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
    public PartnerListResponse getPartners(UUID exhibitionId) {
        exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        List<Partner> partners = partnerRepository.findByExhibitionIdOrderByOrderAscCreatedAtAsc(exhibitionId);
        List<PartnerMember> allMembers = partners.stream()
                .flatMap(p -> partnerMemberRepository.findByPartnerId(p.getId()).stream())
                .collect(java.util.stream.Collectors.toList());

        return PartnerListResponse.from(partners, allMembers);
    }

    @Override
    public PartnerPartCreateResponse createPart(UUID exhibitionId, PartnerPartCreateRequest request) {
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        Partner partner = Partner.builder()
                .exhibition(exhibition)
                .name(request.getPartName())
                .order(request.getOrder())
                .build();

        partnerRepository.save(partner);

        return PartnerPartCreateResponse.from(partner);
    }

    @Override
    public PartnerPartUpdateResponse updatePart(UUID partId, PartnerPartUpdateRequest request) {
        Partner partner = partnerRepository.findById(partId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.PARTNER_NOT_FOUND));

        partner.update(request.getPartName(), request.getOrder());

        return PartnerPartUpdateResponse.from(partner);
    }

    @Override
    public void deletePart(UUID partId) {
        Partner partner = partnerRepository.findById(partId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.PARTNER_NOT_FOUND));

        partnerRepository.delete(partner);
    }

    @Override
    public PartnerMemberCreateResponse createMember(UUID partId, PartnerMemberCreateRequest request) {
        Partner partner = partnerRepository.findById(partId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.PARTNER_NOT_FOUND));

        PartnerMember member = PartnerMember.builder()
                .partner(partner)
                .name(request.getMemberName())
                .imageUrl(request.getMemberImageUrl())
                .email(request.getMemberEmail())
                .build();

        partnerMemberRepository.save(member);

        return PartnerMemberCreateResponse.from(member);
    }

    @Override
    public PartnerMemberUpdateResponse updateMember(UUID memberId, PartnerMemberUpdateRequest request) {
        PartnerMember member = partnerMemberRepository.findById(memberId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.PARTNER_MEMBER_NOT_FOUND));

        member.update(request.getMemberName(), request.getMemberEmail());

        return PartnerMemberUpdateResponse.from(member);
    }

    @Override
    public void deleteMember(UUID memberId) {
        PartnerMember member = partnerMemberRepository.findById(memberId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.PARTNER_MEMBER_NOT_FOUND));

        partnerMemberRepository.delete(member);
    }
}
