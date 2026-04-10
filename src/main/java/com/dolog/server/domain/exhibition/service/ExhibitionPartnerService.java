package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.web.dto.request.partner.PartnerPartCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.partner.PartnerPartCreateResponse;

import java.util.UUID;

public interface ExhibitionPartnerService {

    PartnerPartCreateResponse createPart(UUID exhibitionId, PartnerPartCreateRequest request);
}
