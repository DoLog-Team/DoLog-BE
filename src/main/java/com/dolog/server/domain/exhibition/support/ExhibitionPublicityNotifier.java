package com.dolog.server.domain.exhibition.support;

import com.dolog.server.domain.account.entity.Account;
import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.dolog.server.domain.exhibition.repository.ExhibitionDetailRepository;
import com.dolog.server.domain.notification.entity.enums.NotificationType;
import com.dolog.server.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 전시의 공개 여부(isPublic)가 바뀔 때, "전시에만 공개"로 설정돼 있어 그 영향을 직접 받는 작가들에게 알린다.
// 수동 PATCH(ExhibitionServiceImpl.updateExhibition)와 구독 만료 자동 비공개(SubscriptionExpirationScheduler)가 같이 쓴다.
@Component
@RequiredArgsConstructor
public class ExhibitionPublicityNotifier {

    private final ArtworkRepository artworkRepository;
    private final ExhibitionDetailRepository exhibitionDetailRepository;
    private final NotificationService notificationService;

    public void notify(Exhibition exhibition, boolean nowPublic) {
        List<Artwork> affected = artworkRepository
                .findByExhibitionIdAndStatus(exhibition.getId(), ArtworkStatus.PUBLISHED)
                .stream()
                .filter(Artwork::isExhibitionOnly)
                .toList();

        if (affected.isEmpty()) {
            return;
        }

        NotificationType type = nowPublic
                ? NotificationType.EXHIBITION_REPUBLISHED
                : NotificationType.EXHIBITION_UNPUBLISHED;
        Map<String, String> payload = Map.of("exhibitionName", resolveExhibitionName(exhibition));

        Map<UUID, Account> recipients = new LinkedHashMap<>();
        for (Artwork artwork : affected) {
            for (ArtworkArtistMap map : artwork.getArtworkArtistMaps()) {
                Artist artist = map.getArtist();
                if (artist.getAccount() != null) {
                    recipients.putIfAbsent(artist.getAccount().getId(), artist.getAccount());
                }
            }
        }
        recipients.values().forEach(account -> notificationService.send(account, type, payload, exhibition.getId()));
    }

    private String resolveExhibitionName(Exhibition exhibition) {
        return exhibitionDetailRepository.findByExhibitionId(exhibition.getId())
                .map(ExhibitionDetail::getTitle)
                .orElse(exhibition.getSlug());
    }
}
