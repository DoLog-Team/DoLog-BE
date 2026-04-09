package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.Host;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.repository.HostRepository;
import com.dolog.server.domain.exhibition.web.dto.request.ExhibitionHostUpsertRequest;
import com.dolog.server.domain.exhibition.web.dto.response.ExhibitionHostResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ExhibitionHostServiceImpl implements ExhibitionHostService {
    private final HostRepository hostRepository;
    private final ExhibitionRepository exhibitionRepository;

    @Transactional
    @Override
    public ExhibitionHostResponse upsertExhibitionHost(UUID exhibitionId, ExhibitionHostUpsertRequest request) {
        // 1. 전시 존재 확인
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        // 2. 이미 등록된 주최기관이 있는지 확인
        Host host = hostRepository.findByExhibitionId(exhibitionId)
                .map(existingHost -> {
                    // 이미 있으면 정보 수정 (전체 교체)
                    existingHost.update(request.getHost_name(), request.getHost_image_url(), request.getDescription());
                    return existingHost;
                })
                .orElseGet(() -> {
                    // 없으면 새로 생성
                    return Host.builder()
                            .exhibition(exhibition)
                            .name(request.getHost_name())
                            .img(request.getHost_image_url())
                            .description(request.getDescription())
                            .build();
                });

        Host savedHost = hostRepository.save(host);
        return ExhibitionHostResponse.from(savedHost);
    }
}
