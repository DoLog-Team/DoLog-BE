package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionGuideMap;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionGuideMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionZoneRepository;
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
    private final ExhibitionZoneRepository exhibitionZoneRepository;

    @Override
    public void createGuideMaps(UUID exhibitionId, List<ExhibitionGuideMapCreateRequest> requests) {
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        List<ExhibitionGuideMap> guideMaps = requests.stream()
                .map(req -> {
                    // zoneId를 사용해 해당 구역이 존재하는지 바로 확인
                    ExhibitionZone zone = exhibitionZoneRepository.findById(req.getZoneId())
                            .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.ZONE_NOT_FOUND));

                    return ExhibitionGuideMap.builder()
                            .exhibition(exhibition)
                            .zone(zone) // ⭐️ 찾은 zone 객체를 넣어줌
                            .imageUrl(req.getImageUrl())
                            .description(req.getDescription())
                            .build();
                })
                .toList();

        exhibitionGuideMapRepository.saveAll(guideMaps);
    }
}