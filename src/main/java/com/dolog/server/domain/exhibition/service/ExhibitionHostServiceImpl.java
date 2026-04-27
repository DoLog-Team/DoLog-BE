package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.Host;
import com.dolog.server.domain.exhibition.entity.HostSns;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.repository.host.HostRepository;
import com.dolog.server.domain.exhibition.repository.host.HostSnsRepository;
import com.dolog.server.domain.exhibition.web.dto.request.host.ExhibitionHostUpsertRequest;
import com.dolog.server.domain.exhibition.web.dto.request.host.HostSnsRequest;
import com.dolog.server.domain.exhibition.web.dto.response.host.ExhibitionHostDetailResponse;
import com.dolog.server.domain.exhibition.web.dto.response.host.ExhibitionHostResponse;
import com.dolog.server.domain.exhibition.web.dto.response.host.HostSnsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ExhibitionHostServiceImpl implements ExhibitionHostService {
    private final HostRepository hostRepository;
    private final ExhibitionRepository exhibitionRepository;

    private final HostSnsRepository hostSnsRepository;

    // 주최기관 등록/수정
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
                    existingHost.update(
                            request.getHost_name(),
                            request.getHost_image_url(),
                            request.getDescription(),
                            request.getEmail()
                    );
                    return existingHost;
                })
                .orElseGet(() -> {
                    // 없으면 새로 생성
                    return Host.builder()
                            .exhibition(exhibition)
                            .name(request.getHost_name())
                            .img(request.getHost_image_url())
                            .description(request.getDescription())
                            .email(request.getEmail())
                            .build();
                });

        Host savedHost = hostRepository.save(host);
        return ExhibitionHostResponse.from(savedHost);
    }

    // 주최기관 조회
    @Transactional(readOnly = true)
    @Override
    public ExhibitionHostDetailResponse getExhibitionHost(UUID exhibitionId) {

        // 1. Host 조회
        Host host = hostRepository.findByExhibitionId(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.HOST_NOT_FOUND));

        // 2. SNS 조회
        List<HostSnsResponse> snsList = hostSnsRepository.findByHostId(host.getId())
                .stream()
                .map(HostSnsResponse::from)
                .toList();

        // 3. DTO 변환
        return ExhibitionHostDetailResponse.from(host, snsList);
    }

    // SNS 추가
    @Transactional
    @Override
    public HostSnsResponse addHostSns(UUID exhibitionId, HostSnsRequest request) {
        Host host = hostRepository.findByExhibitionId(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.HOST_NOT_FOUND));

        HostSns sns = HostSns.builder()
                .host(host)
                .platformName(request.getPlatformName())
                .url(request.getUrl())
                .build();

        return HostSnsResponse.from(hostSnsRepository.save(sns));
    }

    // 조회
    @Transactional(readOnly = true)
    @Override
    public List<HostSnsResponse> getHostSnsList(UUID exhibitionId) {
        // 1. Host를 먼저 찾고
        Host host = hostRepository.findByExhibitionId(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.HOST_NOT_FOUND));

        // 2. 해당 Host의 SNS들을 조회
        return hostSnsRepository.findByHostId(host.getId()).stream()
                .map(HostSnsResponse::from)
                .toList();
    }

    // 수정
    @Transactional
    @Override
    public List<HostSnsResponse> updateHostSns(UUID snsId, HostSnsRequest request) {
        // 1. 수정할 SNS 항목 찾기
        HostSns sns = hostSnsRepository.findById(snsId)
                .orElseThrow(() -> new RuntimeException("해당 SNS를 찾을 수 없습니다."));

        // 2. 정보 업데이트
        sns.update(request.getPlatformName(), request.getUrl());

        // 3. sns.getHost().getId()를 사용하여 직접 목록 조회 (exhibitionId가 아닌 hostId 사용)
        return hostSnsRepository.findByHostId(sns.getHost().getId())
                .stream()
                .map(HostSnsResponse::from)
                .toList();
    }

    // 삭제
    @Transactional
    @Override
    public List<HostSnsResponse> deleteHostSns(UUID snsId) {
        HostSns sns = hostSnsRepository.findById(snsId)
                .orElseThrow(() -> new RuntimeException("해당 SNS를 찾을 수 없습니다."));

        hostSnsRepository.delete(sns);

        // 삭제 후 남은 목록 반환
        return hostSnsRepository.findByHostId(sns.getHost().getId())
                .stream()
                .map(HostSnsResponse::from)
                .toList();
    }


}
