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
import com.dolog.server.domain.exhibition.web.dto.request.partner.PartnerMemberReorderRequest;
import com.dolog.server.domain.exhibition.web.dto.request.partner.PartnerMemberUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.partner.PartnerListResponse;
import com.dolog.server.domain.exhibition.web.dto.request.partner.PartnerPartCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.partner.PartnerPartUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.partner.PartnerMemberResponse;
import com.dolog.server.domain.exhibition.web.dto.response.partner.PartnerPartResponse;
import com.dolog.server.global.util.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ExhibitionPartnerServiceImpl implements ExhibitionPartnerService {

    private final ExhibitionRepository exhibitionRepository;
    private final PartnerRepository partnerRepository;
    private final PartnerMemberRepository partnerMemberRepository;
    private final FileService fileService;

    @Override
    @Transactional(readOnly = true)
    public PartnerListResponse getPartners(UUID exhibitionId, String sort) {
        exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        List<Partner> partners = partnerRepository.findByExhibitionIdOrderByOrderAscCreatedAtAsc(exhibitionId);
        List<UUID> partnerIds = partners.stream().map(Partner::getId).collect(java.util.stream.Collectors.toList());

        List<PartnerMember> allMembers = "name".equals(sort)
                ? partnerMemberRepository.findByPartnerIdInOrderByNameAsc(partnerIds)
                : partnerMemberRepository.findByPartnerIdInOrderByOrderAscCreatedAtAsc(partnerIds);

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

        String imageUrl = null;

        try {
            imageUrl = fileService.uploadFile(
                    request.getMemberImage(),
                    "partners"
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int nextOrder = request.getMemberOrder() != null
                ? request.getMemberOrder()
                : partnerMemberRepository.findMaxOrderByPartnerId(partId).map(max -> max + 1).orElse(1);

        PartnerMember member = PartnerMember.builder()
                .partner(partner)
                .name(request.getMemberName())
                .nameEn(request.getMemberNameEn())
                .email(request.getMemberEmail())
                .imageUrl(imageUrl)
                .order(nextOrder)
                .build();

        partnerMemberRepository.save(member);

        return PartnerMemberResponse.from(member);
    }

    @Override
    public PartnerMemberResponse updateMember(UUID memberId, PartnerMemberUpdateRequest request) {

        PartnerMember member = partnerMemberRepository.findById(memberId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.PARTNER_MEMBER_NOT_FOUND));

        String imageUrl = member.getImageUrl();

        try {
            if (request.getMemberImage() != null && !request.getMemberImage().isEmpty()) {

                fileService.deleteFile(member.getImageUrl());

                imageUrl = fileService.uploadFile(
                        request.getMemberImage(),
                        "partners"
                );
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        member.update(
                request.getMemberName(),
                request.getMemberNameEn(),
                request.getMemberEmail(),
                imageUrl
        );

        return PartnerMemberResponse.from(member);
    }

    @Override
    public void deleteMember(UUID memberId) {
        PartnerMember member = partnerMemberRepository.findById(memberId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.PARTNER_MEMBER_NOT_FOUND));

        partnerMemberRepository.delete(member);
    }

    @Override
    public void reorderMembers(UUID partId, List<PartnerMemberReorderRequest> requests) {
        partnerRepository.findById(partId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.PARTNER_NOT_FOUND));

        List<UUID> memberIds = requests.stream()
                .map(PartnerMemberReorderRequest::getMemberId)
                .collect(Collectors.toList());

        List<PartnerMember> members = partnerMemberRepository.findAllById(memberIds);

        boolean hasInvalidMember = members.stream()
                .anyMatch(m -> !m.getPartner().getId().equals(partId));
        if (hasInvalidMember) {
            throw new ExhibitionException(ExhibitionErrorCode.PARTNER_MEMBER_NOT_FOUND);
        }

        Map<UUID, PartnerMember> memberMap = members.stream()
                .collect(Collectors.toMap(PartnerMember::getId, m -> m));

        for (PartnerMemberReorderRequest req : requests) {
            PartnerMember member = memberMap.get(req.getMemberId());
            if (member != null) {
                member.updateOrder(req.getOrder());
            }
        }
    }
}
