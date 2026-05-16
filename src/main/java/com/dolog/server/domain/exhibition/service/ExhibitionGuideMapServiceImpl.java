package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionGuideMap;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionGuideMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionZoneRepository;
import com.dolog.server.domain.exhibition.web.dto.request.guideMap.ExhibitionGuideMapCreateRequest;
import com.dolog.server.global.util.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
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
    public void createGuideMap(
            UUID exhibitionId,
            ExhibitionGuideMapCreateRequest request
    ) {

        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() ->
                        new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        ExhibitionZone zone = null;

        // zoneId가 있으면 존 안내도
        // 없으면 전체 공용 안내도
        if (request.getZoneId() != null) {
            zone = exhibitionZoneRepository.findById(request.getZoneId())
                    .orElseThrow(() ->
                            new ExhibitionException(ExhibitionErrorCode.ZONE_NOT_FOUND));
        }

        try {

            String imageUrl =
                    fileService.uploadFile(request.getImage(), "guide-maps");

            ExhibitionGuideMap guideMap = ExhibitionGuideMap.builder()
                    .exhibition(exhibition)
                    .zone(zone)
                    .imageUrl(imageUrl)
                    .description(request.getDescription())
                    .build();

            exhibitionGuideMapRepository.save(guideMap);

        } catch (IOException e) {
            throw new RuntimeException("가이드맵 업로드 실패", e);
        }
    }
}