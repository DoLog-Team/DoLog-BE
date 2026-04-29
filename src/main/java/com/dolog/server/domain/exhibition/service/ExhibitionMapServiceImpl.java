package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionMap;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.web.dto.request.map.ExhibitionMapCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.map.ExhibitionMapUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.map.ExhibitionMapCreateResponse;
import com.dolog.server.domain.exhibition.web.dto.response.map.ExhibitionMapUpdateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ExhibitionMapServiceImpl implements ExhibitionMapService {

    private final ExhibitionMapRepository exhibitionMapRepository;
    private final ExhibitionRepository exhibitionRepository;

    // 전시 장소 정보 등록
    @Override
    public ExhibitionMapCreateResponse createExhibitionMap(UUID exhibitionId, ExhibitionMapCreateRequest request) {
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        if (exhibitionMapRepository.existsByExhibitionId(exhibitionId)) {
            throw new ExhibitionException(ExhibitionErrorCode.EXHIBITION_MAP_ALREADY_EXISTS);
        }

        ExhibitionMap exhibitionMap = ExhibitionMap.builder()
                .exhibition(exhibition)
                .address(request.getAddress())
                .detailLocation(request.getDetailLocation())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

        ExhibitionMap saved = exhibitionMapRepository.save(exhibitionMap);

        return ExhibitionMapCreateResponse.builder()
                .mapId(saved.getId())
                .exhibitionId(exhibition.getId())
                .build();
    }

    // 전시 장소 정보 수정
    @Override
    public ExhibitionMapUpdateResponse updateExhibitionMap(UUID exhibitionId, ExhibitionMapUpdateRequest request) {
        ExhibitionMap exhibitionMap = exhibitionMapRepository.findByExhibitionId(exhibitionId)
                .orElseThrow(() -> {
                    if (!exhibitionRepository.existsById(exhibitionId)) {
                        return new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND);
                    }
                    return new ExhibitionException(ExhibitionErrorCode.EXHIBITION_MAP_NOT_FOUND);
                });

        exhibitionMap.update(
                request.getAddress(),
                request.getDetailLocation(),
                request.getLatitude(),
                request.getLongitude()
        );

        return ExhibitionMapUpdateResponse.builder()
                .mapId(exhibitionMap.getId())
                .build();
    }

    // 전시 장소 정보 삭제
    @Override
    public void deleteExhibitionMap(UUID exhibitionId) {
        ExhibitionMap exhibitionMap = exhibitionMapRepository.findByExhibitionId(exhibitionId)
                .orElseThrow(() -> {
                    if (!exhibitionRepository.existsById(exhibitionId)) {
                        return new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND);
                    }
                    return new ExhibitionException(ExhibitionErrorCode.EXHIBITION_MAP_NOT_FOUND);
                });

        exhibitionMapRepository.delete(exhibitionMap);
    }
}
