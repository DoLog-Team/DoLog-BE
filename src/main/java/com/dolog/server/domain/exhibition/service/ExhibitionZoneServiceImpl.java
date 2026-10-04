package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.dolog.server.domain.exhibition.repository.ExhibitionDetailRepository;
import com.dolog.server.domain.notification.entity.enums.NotificationType;
import com.dolog.server.domain.notification.service.NotificationService;
import com.dolog.server.global.util.TextUtils;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionZoneRepository;
import com.dolog.server.domain.exhibition.web.dto.request.zone.ExhibitionZoneCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.zone.ExhibitionZoneUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.zone.ExhibitionZoneCreateResponse;
import com.dolog.server.domain.exhibition.web.dto.response.zone.ExhibitionZoneListResponse;
import com.dolog.server.domain.exhibition.web.dto.response.zone.ExhibitionZoneUpdateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ExhibitionZoneServiceImpl implements ExhibitionZoneService {

    private final ExhibitionRepository exhibitionRepository;
    private final ExhibitionZoneRepository exhibitionZoneRepository;
    private final NotificationService notificationService;
    private final ExhibitionDetailRepository exhibitionDetailRepository;

    @Override
    @Transactional(readOnly = true)
    public ExhibitionZoneListResponse getZones(UUID exhibitionId) {
        exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        // orderId 순
        List<ExhibitionZone> sortedZones = exhibitionZoneRepository.findByExhibitionIdOrderByOrderIdAsc(exhibitionId);

        return ExhibitionZoneListResponse.from(sortedZones);
    }

    @Override
    public ExhibitionZoneCreateResponse createZone(UUID exhibitionId, ExhibitionZoneCreateRequest request) {
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        ExhibitionZone zone = ExhibitionZone.builder()
                .exhibition(exhibition)
                .name(request.getName())
                .description(TextUtils.normalizeNewlines(request.getDescription()))
                .orderId(request.getOrderId())
                .build();

        exhibitionZoneRepository.save(zone);

        notificationService.notifyJoinedArtists(
                exhibitionId,
                NotificationType.ZONE_CREATED,
                Map.of("exhibitionName", resolveExhibitionName(exhibition), "zoneName", zone.getName()),
                exhibitionId
        );

        return ExhibitionZoneCreateResponse.from(zone);
    }

    @Override
    public ExhibitionZoneUpdateResponse updateZone(UUID zoneId, ExhibitionZoneUpdateRequest request) {
        ExhibitionZone zone = exhibitionZoneRepository.findById(zoneId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.ZONE_NOT_FOUND));

        zone.update(request.getName(), TextUtils.normalizeNewlines(request.getDescription()), request.getOrderId());

        return ExhibitionZoneUpdateResponse.from(zone);
    }

    @Override
    public void deleteZone(UUID zoneId) {
        ExhibitionZone zone = exhibitionZoneRepository.findById(zoneId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.ZONE_NOT_FOUND));

        UUID exhibitionId = zone.getExhibition().getId();
        String exhibitionName = resolveExhibitionName(zone.getExhibition());
        String zoneName = zone.getName();

        exhibitionZoneRepository.delete(zone);

        notificationService.notifyJoinedArtists(
                exhibitionId,
                NotificationType.ZONE_DELETED,
                Map.of("exhibitionName", exhibitionName, "zoneName", zoneName),
                exhibitionId
        );
    }

    // 전시 이름은 상세 정보의 제목을 쓰고, 없으면 slug로 대신한다
    private String resolveExhibitionName(Exhibition exhibition) {
        return exhibitionDetailRepository.findByExhibitionId(exhibition.getId())
                .map(ExhibitionDetail::getTitle)
                .orElse(exhibition.getSlug());
    }
}
