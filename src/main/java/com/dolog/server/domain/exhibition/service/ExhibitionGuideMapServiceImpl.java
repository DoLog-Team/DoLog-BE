package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionGuideMap;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionGuideMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.web.dto.request.guideMap.ExhibitionGuideMapCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ExhibitionGuideMapServiceImpl implements ExhibitionGuideMapService {

    private final ExhibitionGuideMapRepository exhibitionGuideMapRepository;
    private final ExhibitionRepository exhibitionRepository;

    @Override
    public void createGuideMaps(UUID exhibitionId, List<ExhibitionGuideMapCreateRequest> requests) {
        // 1. 전시회 존재 여부 확인
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        // 2. DTO 리스트를 엔티티 리스트로 변환
        List<ExhibitionGuideMap> guideMaps = requests.stream()
                .map(req -> ExhibitionGuideMap.builder()
                        .exhibition(exhibition)
                        .imageUrl(req.getImageUrl())
                        .description(req.getDescription())
                        .build())
                .toList();

        // 3. 일괄 저장
        exhibitionGuideMapRepository.saveAll(guideMaps);
    }
}