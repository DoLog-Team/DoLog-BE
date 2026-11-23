package com.dolog.server.domain.exhibition.service;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.service.artwork.command.ArtworkPlanLimitService;
import com.dolog.server.domain.artwork.service.artwork.command.PlanLimitRecomputeCause;
import com.dolog.server.domain.artwork.support.ArtworkFieldRequirement;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.dolog.server.domain.notification.entity.enums.NotificationType;
import com.dolog.server.domain.notification.service.NotificationService;
import com.dolog.server.global.util.TextUtils;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionDetailRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionGuideMapRepository;
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

import java.util.ArrayList;
import java.util.HashMap;
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
    private final ExhibitionGuideMapRepository exhibitionGuideMapRepository;
    private final ArtworkFieldRequirement artworkFieldRequirement;
    private final ArtworkPlanLimitService artworkPlanLimitService;

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
        List<Artwork> ungroupedArtworks = removedZones.isEmpty()
                ? List.of()
                : removeZones(exhibition, removedZones);

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

        syncArtworkStatus(exhibition, ungroupedArtworks);

        // 저장 결과를 orderId 순으로 응답
        return ExhibitionZoneListResponse.from(
                exhibitionZoneRepository.findByExhibitionIdOrderByOrderIdAsc(exhibitionId));
    }

    @Override
    public ExhibitionZoneCreateResponse createZone(UUID exhibitionId, UUID accountId, boolean isDologAdmin, ExhibitionZoneCreateRequest request) {
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));
        requireOwnerUnlessDologAdmin(exhibition, accountId, isDologAdmin);

        // orderId를 안 보내면(아직 순서를 안 정한 경우) 기존 구역 중 가장 뒤로 자동 지정한다
        Integer orderId = request.getOrderId() != null
                ? request.getOrderId()
                : exhibitionZoneRepository.findMaxOrderIdByExhibitionId(exhibitionId).map(max -> max + 1).orElse(1);

        ExhibitionZone zone = ExhibitionZone.builder()
                .exhibition(exhibition)
                .name(request.getName())
                .description(TextUtils.normalizeNewlines(request.getDescription()))
                .orderId(orderId)
                .build();

        exhibitionZoneRepository.save(zone);
        syncArtworkStatus(exhibition, List.of());

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

        List<Artwork> ungroupedArtworks = removeZones(zone.getExhibition(), List.of(zone));
        syncArtworkStatus(zone.getExhibition(), ungroupedArtworks);
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

    private List<Artwork> removeZones(Exhibition exhibition, List<ExhibitionZone> zones) {
        String exhibitionName = resolveExhibitionName(exhibition);
        List<Artwork> ungroupedArtworks = new ArrayList<>();

        for (ExhibitionZone zone : zones) {
            List<Artwork> artworks = artworkRepository
                    .findByExhibitionIdAndExhibitionZoneIdOrderByOrderIndexAsc(exhibition.getId(), zone.getId());
            notifyArtworkGroupRemoved(exhibition, exhibitionName, zone, artworks);
            artworks.forEach(artwork -> artwork.updateZone(null));
            ungroupedArtworks.addAll(artworks);
        }

        artworkRepository.clearZone(zones.stream().map(ExhibitionZone::getId).toList());
        exhibitionGuideMapRepository.clearZone(zones);
        exhibitionZoneRepository.deleteAll(zones);
        return ungroupedArtworks;
    }

    // 그룹 저장 뒤 "공개 작품은 필수값을 채운다" 규칙을 맞춘다. 그룹은 전시에 그룹이 하나라도 있을 때만 필수다.
    // - 그룹이 있으면: 그룹 없는 공개 작품을 자동 임시저장 (그룹 삭제로 풀린 작품, 그룹 0개 시절 공개된 작품 모두)
    // - 그룹이 없으면: 자동 임시저장됐던 작품 중 필수값을 채운 작품을 다시 공개
    // ungroupedArtworks 는 이번 저장에서 그룹이 삭제돼 풀린 작품 (그룹 제거 알림을 이미 받았으므로 임시저장 알림은 따로 보내지 않는다)
    private void syncArtworkStatus(Exhibition exhibition, List<Artwork> ungroupedArtworks) {
        UUID exhibitionId = exhibition.getId();
        List<Artwork> changed;

        if (exhibitionZoneRepository.existsByExhibitionId(exhibitionId)) {
            changed = draftUngroupedPublished(exhibitionId);
            List<Artwork> newlyDrafted = changed.stream()
                    .filter(artwork -> !ungroupedArtworks.contains(artwork))
                    .toList();
            notifyArtists(exhibition, newlyDrafted, NotificationType.ARTWORK_UNPUBLISHED, Map.of("requiredFieldNames", "그룹"));
        } else {
            changed = artworkFieldRequirement.republishIfSatisfied(
                    artworkRepository.findByExhibitionIdAndStatusAndAutoDraftedAtIsNotNull(exhibitionId, ArtworkStatus.DRAFT));
            notifyArtists(exhibition, changed, NotificationType.ARTWORK_REPUBLISHED, Map.of());
        }

        if (!changed.isEmpty()) {
            artworkPlanLimitService.recompute(exhibitionId, PlanLimitRecomputeCause.ARTWORK_CHANGE);
        }
    }

    private List<Artwork> draftUngroupedPublished(UUID exhibitionId) {
        List<Artwork> drafted = new ArrayList<>();
        for (Artwork artwork : artworkRepository.findByExhibitionIdAndStatus(exhibitionId, ArtworkStatus.PUBLISHED)) {
            if (artwork.getExhibitionZone() != null) {
                continue;
            }
            artworkFieldRequirement.draftIfUnsatisfied(artwork);
            if (artwork.getStatus() != ArtworkStatus.PUBLISHED) {
                drafted.add(artwork);
            }
        }
        return drafted;
    }

    private void notifyArtists(
            Exhibition exhibition, List<Artwork> artworks, NotificationType type, Map<String, String> extra) {
        if (artworks.isEmpty()) {
            return;
        }
        String exhibitionName = resolveExhibitionName(exhibition);
        for (Artwork artwork : artworks) {
            Map<String, String> payload = new HashMap<>(extra);
            payload.put("exhibitionName", exhibitionName);
            payload.put("artworkTitle", artwork.getTitle());
            payload.put("artworkId", artwork.getId().toString());

            for (ArtworkArtistMap map : artwork.getArtworkArtistMaps()) {
                Artist artist = map.getArtist();
                if (artist.getAccount() == null) {
                    continue;
                }
                notificationService.send(artist.getAccount(), type, payload, exhibition.getId());
            }
        }
    }

    private void notifyArtworkGroupRemoved(
            Exhibition exhibition, String exhibitionName, ExhibitionZone zone, List<Artwork> artworks) {
        for (Artwork artwork : artworks) {
            Map<String, String> payload = Map.of(
                    "exhibitionName", exhibitionName,
                    "artworkTitle", artwork.getTitle(),
                    "artworkId", artwork.getId().toString(),
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
