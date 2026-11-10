package com.dolog.server.domain.artwork.service.artwork.command;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.repository.ArtworkSpecification;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.notification.entity.enums.NotificationType;
import com.dolog.server.domain.notification.service.NotificationService;
import com.dolog.server.domain.plan.entity.Subscription;
import com.dolog.server.domain.plan.entity.enums.SubscriptionStatus;
import com.dolog.server.domain.plan.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 전시의 작품 노출을 "현재 플랜의 작품 노출 한도"에 맞게 다시 계산한다.
// 출품/등록 순서(createdAt) 기준으로 한도 안은 공개, 넘는 분은 자동 미노출(plan_limit_exceeded_at)로 바꾼다.
// 전시 동선 순서(구역 순서)는 진열용 순서일 뿐이고, 한도 초과는 "가장 늦게 출품한 작품"이어야 하므로 동선 순서를 쓰지 않는다 (기획 확인).
// 관리자가 직접 숨긴 작품(hiddenAt)은 애초에 대상에서 빠지므로, 숨기면 뒤 작품이 자동으로 당겨 올라온다.
// cause는 호출 원인(PLAN_CHANGE/ARTWORK_CHANGE)을 표시만 해둔다
@Service
@RequiredArgsConstructor
public class ArtworkPlanLimitService {

    private final ArtworkRepository artworkRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final NotificationService notificationService;

    @Transactional
    public void recompute(UUID exhibitionId, PlanLimitRecomputeCause cause) {
        Integer limit = resolveActiveLimit(exhibitionId);

        // 활성 구독이 없거나 한도가 없는 플랜이면 검사하지 않는다 (무제한 취급) — 기존에 걸려 있던 미노출은 풀어준다.
        if (limit == null) {
            artworkRepository.clearPlanLimitExceededByExhibitionId(exhibitionId);
            return;
        }

        // 노출 순서(동선)가 아니라 출품/등록 순서 기준으로 한도를 판단한다 — 한도를 넘기는 건 "새로 출품한 작품"이어야 한다.
        List<Artwork> candidates = artworkRepository.findAll(
                Specification.where(ArtworkSpecification.withExhibitionId(exhibitionId))
                        .and(ArtworkSpecification.withHidden(false)),
                Sort.by(Sort.Direction.ASC, "createdAt")
        );

        List<Artwork> newlyExceeded = new java.util.ArrayList<>();
        List<Artwork> newlyCleared = new java.util.ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < candidates.size(); i++) {
            Artwork artwork = candidates.get(i);
            boolean withinLimit = i < limit;

            if (withinLimit && artwork.exceedsPlanLimit()) {
                artwork.clearPlanLimitExceeded();
                newlyCleared.add(artwork);
            } else if (!withinLimit && !artwork.exceedsPlanLimit()) {
                artwork.markPlanLimitExceeded(now);
                newlyExceeded.add(artwork);
            }
        }

        if (!newlyExceeded.isEmpty()) {
            long hiddenCount = candidates.stream().filter(Artwork::exceedsPlanLimit).count();
            notifyExceeded(exhibitionId, candidates.get(0).getExhibition(), hiddenCount);

            // 작가 본인의 출품으로 한도를 넘긴 경우에만 작가에게 개별 알린다
            if (cause == PlanLimitRecomputeCause.ARTWORK_CHANGE) {
                newlyExceeded.forEach(artwork -> notifyArtistExceeded(exhibitionId, artwork));
            }
        }

        if (!newlyCleared.isEmpty()) {
            if (cause == PlanLimitRecomputeCause.ARTWORK_CHANGE) {
                // 다른 작품이 숨겨지거나 연결 해제돼서 자리가 난 경우 — 작품별로 각각 알린다
                newlyCleared.forEach(artwork -> notifyCleared(exhibitionId, artwork));
            } else {
                // 플랜 업그레이드로 자리가 난 경우 — 전시·작가 기준으로 묶어서 1건만 알린다 
                notifyPlanUpgradeCleared(exhibitionId, newlyCleared);
            }
        }
    }

    // 활성 구독이 없거나, 플랜에 작품 수 한도가 없으면 null (= 검사 안 함)
    private Integer resolveActiveLimit(UUID exhibitionId) {
        return subscriptionRepository
                .findFirstByExhibitionIdAndStatusInOrderByCreatedAtDesc(
                        exhibitionId, List.of(SubscriptionStatus.ACTIVE))
                .map(Subscription::getPlan)
                .map(plan -> plan.getMaxArtworkCount())
                .orElse(null);
    }

    private void notifyExceeded(UUID exhibitionId, Exhibition exhibition, long hiddenCount) {
        notificationService.send(
                exhibition.getAccount(),
                NotificationType.PLAN_LIMIT_EXCEEDED,
                Map.of(
                        "exhibitionName", resolveExhibitionName(exhibition),
                        "hiddenArtworkCount", String.valueOf(hiddenCount)
                ),
                exhibitionId
        );
    }

    // 플랜 한도 자리가 확보되어 다시 노출된 작품의 작가들에게 알린다 (계정 없는 작가는 건너뜀)
    private void notifyCleared(UUID exhibitionId, Artwork artwork) {
        Map<String, String> payload = Map.of(
                "exhibitionName", resolveExhibitionName(artwork.getExhibition()),
                "artworkTitle", artwork.getTitle(),
                "artworkId", artwork.getId().toString()
        );
        for (ArtworkArtistMap map : artwork.getArtworkArtistMaps()) {
            Artist artist = map.getArtist();
            if (artist.getAccount() != null) {
                notificationService.send(artist.getAccount(), NotificationType.ARTWORK_PLAN_LIMIT_CLEARED, payload, exhibitionId);
            }
        }
    }

    // 작가가 작품을 출품했는데 플랜 한도를 넘겨 미노출된 경우, 그 작가들에게 알린다 (계정 없는 작가는 건너뜀)
    private void notifyArtistExceeded(UUID exhibitionId, Artwork artwork) {
        Map<String, String> payload = Map.of(
                "exhibitionName", resolveExhibitionName(artwork.getExhibition()),
                "artworkTitle", artwork.getTitle(),
                "artworkId", artwork.getId().toString()
        );
        for (ArtworkArtistMap map : artwork.getArtworkArtistMaps()) {
            Artist artist = map.getArtist();
            if (artist.getAccount() != null) {
                notificationService.send(artist.getAccount(), NotificationType.ARTWORK_PLAN_LIMIT_EXCEEDED, payload, exhibitionId);
            }
        }
    }

    // 플랜 업그레이드로 자리가 나 공개로 전환된 작품들을, 작가별로 묶어서(그 작가의 작품만) 한 건씩 알린다
    private void notifyPlanUpgradeCleared(UUID exhibitionId, List<Artwork> clearedArtworks) {
        String exhibitionName = resolveExhibitionName(clearedArtworks.get(0).getExhibition());

        Map<UUID, com.dolog.server.domain.account.entity.Account> recipients = new java.util.LinkedHashMap<>();
        Map<UUID, List<String>> titlesByAccountId = new java.util.LinkedHashMap<>();

        for (Artwork artwork : clearedArtworks) {
            for (ArtworkArtistMap map : artwork.getArtworkArtistMaps()) {
                Artist artist = map.getArtist();
                if (artist.getAccount() == null) {
                    continue;
                }
                UUID accountId = artist.getAccount().getId();
                recipients.putIfAbsent(accountId, artist.getAccount());
                titlesByAccountId.computeIfAbsent(accountId, k -> new java.util.ArrayList<>()).add(artwork.getTitle());
            }
        }

        recipients.forEach((accountId, account) -> {
            List<String> titles = titlesByAccountId.get(accountId);
            Map<String, String> payload = Map.of(
                    "exhibitionName", exhibitionName,
                    "artworkTitles", String.join(", ", titles),
                    "artworkCount", String.valueOf(titles.size())
            );
            notificationService.send(account, NotificationType.PLAN_UPGRADE_ARTWORKS_PUBLISHED, payload, exhibitionId);
        });
    }

    private String resolveExhibitionName(Exhibition exhibition) {
        return exhibition.getExhibitionDetail() != null
                ? exhibition.getExhibitionDetail().getTitle()
                : exhibition.getSlug();
    }
}
