package com.dolog.server.domain.artwork.service.artwork.command;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.notification.entity.enums.NotificationType;
import com.dolog.server.domain.notification.service.NotificationService;
import com.dolog.server.domain.plan.entity.Subscription;
import com.dolog.server.domain.plan.entity.enums.SubscriptionStatus;
import com.dolog.server.domain.plan.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 전시의 작품 노출을 "현재 플랜의 작품 노출 한도"에 맞게 다시 계산한다.
// 연결 순서(orderIndex) 기준으로 한도 안은 공개, 넘는 분은 자동 미노출(plan_limit_exceeded_at)로 바꾼다.
// 관리자가 직접 숨긴 작품(hiddenAt)은 애초에 대상에서 빠지므로, 숨기면 뒤 작품이 자동으로 당겨 올라온다.
@Service
@RequiredArgsConstructor
public class ArtworkPlanLimitService {

    private final ArtworkRepository artworkRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final NotificationService notificationService;

    @Transactional
    public void recompute(UUID exhibitionId) {
        Integer limit = resolveActiveLimit(exhibitionId);

        // 활성 구독이 없거나 한도가 없는 플랜이면 검사하지 않는다 (무제한 취급) — 기존에 걸려 있던 미노출은 풀어준다.
        if (limit == null) {
            artworkRepository.clearPlanLimitExceededByExhibitionId(exhibitionId);
            return;
        }

        List<Artwork> candidates = artworkRepository
                .findByExhibitionIdAndHiddenAtIsNullOrderByOrderIndexAsc(exhibitionId);

        boolean newlyExceeded = false;
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < candidates.size(); i++) {
            Artwork artwork = candidates.get(i);
            boolean withinLimit = i < limit;

            if (withinLimit && artwork.exceedsPlanLimit()) {
                artwork.clearPlanLimitExceeded();
            } else if (!withinLimit && !artwork.exceedsPlanLimit()) {
                artwork.markPlanLimitExceeded(now);
                newlyExceeded = true;
            }
        }

        if (newlyExceeded) {
            notifyExceeded(exhibitionId, candidates.get(0).getExhibition());
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

    private void notifyExceeded(UUID exhibitionId, Exhibition exhibition) {
        notificationService.send(
                exhibition.getAccount(),
                NotificationType.PLAN_LIMIT_EXCEEDED,
                Map.of("exhibitionName", resolveExhibitionName(exhibition)),
                exhibitionId
        );
    }

    private String resolveExhibitionName(Exhibition exhibition) {
        return exhibition.getExhibitionDetail() != null
                ? exhibition.getExhibitionDetail().getTitle()
                : exhibition.getSlug();
    }
}
