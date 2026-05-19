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


    /**
     * 가이드맵 수정 (PATCH)
     */
    @Override
    public void updateGuideMap(UUID guideMapId, ExhibitionGuideMapCreateRequest request) {
        // 1. 기존 가이드맵 조회
        ExhibitionGuideMap guideMap = exhibitionGuideMapRepository.findById(guideMapId)
                .orElseThrow(() -> new RuntimeException("가이드맵을 찾을 수 없습니다.")); // 프로젝트 예외 클래스로 변경 가능

        // 2. 이미지 처리 로직
        String newImageUrl = guideMap.getImageUrl(); // 기본값은 기존 이미지 유지

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            try {
                if (guideMap.getImageUrl() != null) {
                    fileService.deleteFile(guideMap.getImageUrl());
                }
                newImageUrl = fileService.uploadFile(request.getImage(), "guide-maps");
            } catch (IOException e) {
                throw new RuntimeException("가이드맵 파일 수정 실패", e);
            }
        }

        // 3. Zone 정보 수정 (선택적)
        ExhibitionZone zone = guideMap.getZone();
        if (request.getZoneId() != null) {
            zone = exhibitionZoneRepository.findById(request.getZoneId())
                    .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.ZONE_NOT_FOUND));
        }

        // 4. 엔티티 정보 업데이트
        // ExhibitionGuideMap 엔티티에 update 가이드맵용 메서드가 정의되어 있어야 합니다.
        guideMap.updateGuideMap(
                zone,
                newImageUrl,
                request.getDescription() != null ? request.getDescription() : guideMap.getDescription()
        );
    }

    /**
     * 가이드맵 삭제 (DELETE)
     */
    @Override
    public void deleteGuideMap(UUID guideMapId) {
        // 1. 기존 가이드맵 조회
        ExhibitionGuideMap guideMap = exhibitionGuideMapRepository.findById(guideMapId)
                .orElseThrow(() -> new RuntimeException("가이드맵을 찾을 수 없습니다."));

        // 2. 스토리지 파일 선 삭제
        if (guideMap.getImageUrl() != null) {
            fileService.deleteFile(guideMap.getImageUrl());
        }

        // 3. DB 데이터 삭제
        exhibitionGuideMapRepository.delete(guideMap);
    }
}