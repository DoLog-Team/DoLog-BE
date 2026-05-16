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
import com.dolog.server.global.util.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ExhibitionGuideMapServiceImpl implements ExhibitionGuideMapService {

    private final ExhibitionGuideMapRepository exhibitionGuideMapRepository;
    private final ExhibitionRepository exhibitionRepository;
    private final ExhibitionZoneRepository exhibitionZoneRepository;
    private final FileService fileService;

    @Override
    public void createGuideMaps(UUID exhibitionId, List<ExhibitionGuideMapCreateRequest.GuideMapRequest> requests) {
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        List<ExhibitionGuideMap> guideMaps = requests.stream()
                .map(req -> {
                    // 구역 조회
                    ExhibitionZone zone = null;

                    if (req.getZoneId() != null) {
                        zone = exhibitionZoneRepository.findById(req.getZoneId())
                                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.ZONE_NOT_FOUND));
                    }

                    String imageUrl = null;
                    try {
                        imageUrl = fileService.uploadFile(req.getImage(), "guide-maps");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    return ExhibitionGuideMap.builder()
                            .exhibition(exhibition)
                            .zone(zone)
                            .imageUrl(imageUrl)
                            .description(req.getDescription())
                            .build();
                })
                .toList();

        exhibitionGuideMapRepository.saveAll(guideMaps);
    }
}