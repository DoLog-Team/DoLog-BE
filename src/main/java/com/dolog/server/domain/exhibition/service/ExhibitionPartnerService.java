package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.web.dto.request.partner.PartnerMemberCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.partner.PartnerMemberUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.partner.PartnerListResponse;
import com.dolog.server.domain.exhibition.web.dto.request.partner.PartnerPartCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.partner.PartnerPartUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.partner.PartnerMemberResponse;
import com.dolog.server.domain.exhibition.web.dto.response.partner.PartnerPartResponse;

import java.util.UUID;

public interface ExhibitionPartnerService {

    PartnerListResponse getPartners(UUID exhibitionId);

    PartnerPartResponse createPart(UUID exhibitionId, PartnerPartCreateRequest request);

    PartnerPartResponse updatePart(UUID partId, PartnerPartUpdateRequest request);

    void deletePart(UUID partId);

    PartnerMemberResponse createMember(UUID partId, PartnerMemberCreateRequest request);

    PartnerMemberResponse updateMember(UUID memberId, PartnerMemberUpdateRequest request);

    void deleteMember(UUID memberId);
}
