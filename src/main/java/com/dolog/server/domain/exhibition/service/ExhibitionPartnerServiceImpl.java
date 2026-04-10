package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.Partner;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.repository.PartnerRepository;
import com.dolog.server.domain.exhibition.web.dto.request.partner.PartnerPartCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.partner.PartnerPartCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ExhibitionPartnerServiceImpl implements ExhibitionPartnerService {

    private final ExhibitionRepository exhibitionRepository;
    private final PartnerRepository partnerRepository;

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
}
