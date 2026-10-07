package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.dolog.server.domain.notification.entity.enums.NotificationType;
import com.dolog.server.domain.notification.service.NotificationService;
import com.dolog.server.global.util.TextUtils;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionDetailRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionZoneRepository;
import com.dolog.server.domain.exhibition.web.dto.request.zone.ExhibitionZoneBulkSaveRequest;
import com.dolog.server.domain.exhibition.web.dto.request.zone.ExhibitionZoneCreateRequest;
import com.dolog.server.domain.exhibition.web.dto.request.zone.ExhibitionZoneUpdateRequest;
import com.dolog.server.domain.exhibition.web.dto.response.zone.ExhibitionZoneCreateResponse;
import com.dolog.server.domain.exhibition.web.dto.response.zone.ExhibitionZoneHiddenUpdateResponse;
import com.dolog.server.domain.exhibition.web.dto.response.zone.ExhibitionZoneListResponse;
import com.dolog.server.domain.exhibition.web.dto.response.zone.ExhibitionZoneUpdateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ExhibitionZoneServiceImpl implements ExhibitionZoneService {

    private final ExhibitionRepository exhibitionRepository;
    private final ExhibitionZoneRepository exhibitionZoneRepository;
    private final ExhibitionDetailRepository exhibitionDetailRepository;
    private final NotificationService notificationService;
    private final ArtworkRepository artworkRepository;

    @Override
    @Transactional(readOnly = true)
    public ExhibitionZoneListResponse getZones(UUID exhibitionId) {
        exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        // orderId 순
        List<ExhibitionZone> sortedZones = exhibitionZoneRepository.findByExhibitionIdOrderByOrderIdAsc(exhibitionId);

        return ExhibitionZoneListResponse.from(sortedZones);
    }

    // 두록 어드민은 모든 전시를, 전시 어드민은 본인 전시만 다룰 수 있다
    private void requireOwnerUnlessDologAdmin(Exhibition exhibition, UUID accountId, boolean isDologAdmin) {
        if (isDologAdmin) {
            return;
        }
        if (!exhibition.getAccount().getId().equals(accountId)) {
            throw new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_OWNER);
        }
    }

    // 작품 그룹 일괄 저장: 요청 목록으로 전체를 교체한다
    @Override
    public ExhibitionZoneListResponse saveZones(UUID exhibitionId, UUID accountId, ExhibitionZoneBulkSaveRequest request) {
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        if (!exhibition.getAccount().getId().equals(accountId)) {
            throw new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_OWNER);
        }

        List<ExhibitionZone> existingZones = exhibitionZoneRepository.findByExhibitionId(exhibitionId);
        Map<UUID, ExhibitionZone> existingById = existingZones.stream()
                .collect(Collectors.toMap(ExhibitionZone::getId, Function.identity()));

        // 요청에 포함된 기존 id는 반드시 이 전시의 zone이어야 한다
        Set<UUID> requestedIds = request.getZones().stream()
                .map(ExhibitionZoneBulkSaveRequest.ZoneItem::getId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (!existingById.keySet().containsAll(requestedIds)) {
            throw new ExhibitionException(ExhibitionErrorCode.ZONE_NOT_FOUND);
        }

        // 요청 목록에 없는 기존 zone은 삭제하고, 소속 작품은 zone을 해제한다
        List<ExhibitionZone> removedZones = existingZones.stream()
                .filter(zone -> !requestedIds.contains(zone.getId()))
                .toList();
        if (!removedZones.isEmpty()) {
            notifyZoneRemoved(exhibition, removedZones);
            artworkRepository.clearZone(removedZones);
            exhibitionZoneRepository.deleteAll(removedZones);
        }

        for (ExhibitionZoneBulkSaveRequest.ZoneItem item : request.getZones()) {
            String description = TextUtils.normalizeNewlines(item.getDescription());
            if (item.getId() == null) {
                exhibitionZoneRepository.save(ExhibitionZone.builder()
                        .exhibition(exhibition)
                        .name(item.getName())
                        .description(description)
                        .orderId(item.getOrderId())
                        .build());
            } else {
                existingById.get(item.getId()).replace(item.getName(), description, item.getOrderId());
            }
        }

        // 저장 결과를 orderId 순으로 응답
        return ExhibitionZoneListResponse.from(
                exhibitionZoneRepository.findByExhibitionIdOrderByOrderIdAsc(exhibitionId));
    }

    @Override
    public ExhibitionZoneCreateResponse createZone(UUID exhibitionId, UUID accountId, boolean isDologAdmin, ExhibitionZoneCreateRequest request) {
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));
        requireOwnerUnlessDologAdmin(exhibition, accountId, isDologAdmin);

        ExhibitionZone zone = ExhibitionZone.builder()
                .exhibition(exhibition)
                .name(request.getName())
                .description(TextUtils.normalizeNewlines(request.getDescription()))
                .orderId(request.getOrderId())
                .build();

        exhibitionZoneRepository.save(zone);

        return ExhibitionZoneCreateResponse.from(zone);
    }

    @Override
    public ExhibitionZoneUpdateResponse updateZone(UUID zoneId, UUID accountId, boolean isDologAdmin, ExhibitionZoneUpdateRequest request) {
        ExhibitionZone zone = exhibitionZoneRepository.findById(zoneId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.ZONE_NOT_FOUND));
        requireOwnerUnlessDologAdmin(zone.getExhibition(), accountId, isDologAdmin);

        zone.update(request.getName(), TextUtils.normalizeNewlines(request.getDescription()), request.getOrderId());

        return ExhibitionZoneUpdateResponse.from(zone);
    }

    @Override
    public void deleteZone(UUID zoneId, UUID accountId, boolean isDologAdmin) {
        ExhibitionZone zone = exhibitionZoneRepository.findById(zoneId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.ZONE_NOT_FOUND));
        requireOwnerUnlessDologAdmin(zone.getExhibition(), accountId, isDologAdmin);

        exhibitionZoneRepository.delete(zone);
    }

    // 작품 그룹 숨김/재공개. 상태가 실제로 바뀐 경우에만 참여 작가에게 알림을 보낸다
    @Override
    public ExhibitionZoneHiddenUpdateResponse changeHidden(UUID zoneId, UUID accountId, boolean isDologAdmin, boolean hidden) {
        ExhibitionZone zone = exhibitionZoneRepository.findById(zoneId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.ZONE_NOT_FOUND));

        Exhibition exhibition = zone.getExhibition();
        if (!isDologAdmin && !exhibition.getAccount().getId().equals(accountId)) {
            throw new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_OWNER);
        }

        boolean changed = zone.isHidden() != hidden;
        zone.changeHidden(hidden);

        if (changed) {
            notificationService.notifyJoinedArtists(
                    exhibition.getId(),
                    hidden ? NotificationType.ZONE_HIDDEN : NotificationType.ZONE_SHOWN,
                    Map.of("exhibitionName", resolveExhibitionName(exhibition), "zoneName", zone.getName()),
                    exhibition.getId()
            );
        }

        return ExhibitionZoneHiddenUpdateResponse.from(zone);
    }

    // 전시 이름은 상세 정보의 제목을 쓰고, 없으면 slug로 대신한다
    private String resolveExhibitionName(Exhibition exhibition) {
        return exhibitionDetailRepository.findByExhibitionId(exhibition.getId())
                .map(ExhibitionDetail::getTitle)
                .orElse(exhibition.getSlug());
    }

    // 삭제될 그룹에 속해 있던 작품들의 작가 전원에게, zone이 실제로 비워지기 전에 알린다
    private void notifyZoneRemoved(Exhibition exhibition, List<ExhibitionZone> removedZones) {
        String exhibitionName = resolveExhibitionName(exhibition);

        for (ExhibitionZone zone : removedZones) {
            List<Artwork> artworks = artworkRepository
                    .findByExhibitionIdAndExhibitionZoneIdOrderByOrderIndexAsc(exhibition.getId(), zone.getId());

            for (Artwork artwork : artworks) {
                Map<String, String> payload = Map.of(
                        "exhibitionName", exhibitionName,
                        "artworkTitle", artwork.getTitle(),
                        "zoneName", zone.getName()
                );

                for (ArtworkArtistMap map : artwork.getArtworkArtistMaps()) {
                    Artist artist = map.getArtist();
                    if (artist.getAccount() == null) {
                        continue;
                    }
                    notificationService.send(
                            artist.getAccount(), NotificationType.ARTWORK_GROUP_REMOVED, payload, exhibition.getId());
                }
            }
        }
    }
}
