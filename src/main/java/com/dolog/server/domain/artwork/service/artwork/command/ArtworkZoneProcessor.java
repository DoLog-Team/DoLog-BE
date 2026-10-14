package com.dolog.server.domain.artwork.service.artwork.command;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.service.order.ArtworkOrderAdapter;
import com.dolog.server.domain.artwork.support.ArtworkValidator;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkBulkZoneResponse;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.domain.notification.entity.enums.NotificationType;
import com.dolog.server.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtworkZoneProcessor {

    private final ArtworkValidator artworkValidator;
    private final ArtworkOrderAdapter artworkOrderAdapter;
    private final NotificationService notificationService;
    private final ArtworkPlanLimitService artworkPlanLimitService;

    // 하나라도 이 전시 출품작이 아니면 아무것도 바꾸지 않는다
    public ArtworkBulkZoneResponse assignZone(
            UUID accountId,
            UUID exhibitionId,
            List<UUID> artworkIds,
            UUID zoneId
    ) {

        Exhibition exhibition = artworkValidator.getOwnedExhibition(accountId, exhibitionId);
        ExhibitionZone zone = artworkValidator.validateZone(zoneId, exhibitionId);
        List<Artwork> artworks = artworkValidator.getArtworksInExhibition(exhibitionId, artworkIds);

        int changedCount = 0;
        for (Artwork artwork : artworks) {
            if (zoneId.equals(currentZoneId(artwork))) {
                continue;
            }

            artwork.updateZone(zone);
            artworkOrderAdapter.assign(artwork);
            notifyArtists(artwork, exhibition, zone);
            changedCount++;
        }

        // 순서(orderIndex)가 바뀌면 플랜 한도 안팎이 달라질 수 있다
        if (changedCount > 0) {
            artworkPlanLimitService.recompute(exhibitionId);
        }

        return new ArtworkBulkZoneResponse(
                zone.getId(),
                zone.getName(),
                changedCount,
                artworks.stream().map(Artwork::getId).toList()
        );
    }

    private UUID currentZoneId(Artwork artwork) {
        return artwork.getExhibitionZone() != null ? artwork.getExhibitionZone().getId() : null;
    }

    // 계정 없는 작가는 건너뛴다
    private void notifyArtists(Artwork artwork, Exhibition exhibition, ExhibitionZone zone) {
        Map<String, String> payload = Map.of(
                "exhibitionName", resolveExhibitionName(exhibition),
                "artworkTitle", artwork.getTitle(),
                "zoneName", zone.getName()
        );

        for (ArtworkArtistMap map : artwork.getArtworkArtistMaps()) {
            Artist artist = map.getArtist();
            if (artist.getAccount() == null) {
                continue;
            }
            notificationService.send(artist.getAccount(), NotificationType.ARTWORK_GROUP_ASSIGNED, payload, exhibition.getId());
        }
    }

    // 전시 이름은 상세 정보의 제목을 쓰고, 없으면 slug로 대신한다
    private String resolveExhibitionName(Exhibition exhibition) {
        return exhibition.getExhibitionDetail() != null
                ? exhibition.getExhibitionDetail().getTitle()
                : exhibition.getSlug();
    }
}
