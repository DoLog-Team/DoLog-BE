package com.dolog.server.domain.artwork.service.artwork.command;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.support.ArtworkFieldRequirement;
import com.dolog.server.domain.artwork.support.ArtworkValidator;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkBulkHiddenResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkExhibitionOnlyResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkHiddenResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkStatusResponse;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.notification.entity.enums.NotificationType;
import com.dolog.server.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtworkVisibilityProcessor {

    private final ArtworkRepository artworkRepository;
    private final ArtworkValidator artworkValidator;
    private final ArtworkFieldRequirement artworkFieldRequirement;
    private final NotificationService notificationService;
    private final ArtworkPlanLimitService artworkPlanLimitService;

    // 작가 본인의 공개/임시저장 전환. 전시 어드민의 숨김(hidden_at)은 건드리지 않는다.
    public ArtworkStatusResponse changeStatus(
            UUID accountId,
            UUID artworkId,
            ArtworkStatus status
    ) {

        Artist artist = artworkValidator.getLoginArtist(accountId);
        Artwork artwork = artworkValidator.getOwnedArtwork(artworkId, artist);

        if (status == ArtworkStatus.PUBLISHED && !artworkFieldRequirement.isSatisfied(artwork)) {
            throw new ArtworkException(ArtworkErrorCode.REQUIRED_FIELDS_MISSING);
        }

        artwork.changeStatus(status);

        return new ArtworkStatusResponse(artwork.getId(), artwork.getStatus());
    }

    // 작가 본인의 "전시에서만 공개" 토글. 전시 노출(isVisibleOnExhibition)은 그대로 유지된다.
    public ArtworkExhibitionOnlyResponse changeExhibitionOnly(
            UUID accountId,
            UUID artworkId,
            boolean exhibitionOnly
    ) {

        Artist artist = artworkValidator.getLoginArtist(accountId);
        Artwork artwork = artworkValidator.getOwnedArtwork(artworkId, artist);

        artwork.updateExhibitionOnly(exhibitionOnly);

        return new ArtworkExhibitionOnlyResponse(artwork.getId(), artwork.isExhibitionOnly());
    }

    // 전시 어드민의 숨김/재공개. 공개 상태(status)는 건드리지 않는다.
    public ArtworkHiddenResponse changeHidden(
            UUID accountId,
            UUID artworkId,
            boolean hidden
    ) {

        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        if (artwork.getExhibition() == null) {
            throw new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_SUBMITTED);
        }

        if (!artwork.getExhibition().getAccount().getId().equals(accountId)) {
            throw new ArtworkException(ArtworkErrorCode.NOT_EXHIBITION_OWNER);
        }

        if (applyHidden(artwork, hidden)) {
            artworkPlanLimitService.recompute(artwork.getExhibition().getId());
            notifyArtists(artwork, hiddenNotificationType(hidden));
        }

        return toHiddenResponse(artwork);
    }

    // 하나라도 이 전시 출품작이 아니면 아무것도 바꾸지 않는다
    public ArtworkBulkHiddenResponse changeHiddenBulk(
            UUID accountId,
            UUID exhibitionId,
            List<UUID> artworkIds,
            boolean hidden
    ) {

        artworkValidator.getOwnedExhibition(accountId, exhibitionId);
        List<Artwork> artworks = artworkValidator.getArtworksInExhibition(exhibitionId, artworkIds);

        List<Artwork> changed = artworks.stream()
                .filter(artwork -> applyHidden(artwork, hidden))
                .toList();

        if (!changed.isEmpty()) {
            artworkPlanLimitService.recompute(exhibitionId);
            changed.forEach(artwork -> notifyArtists(artwork, hiddenNotificationType(hidden)));
        }

        return new ArtworkBulkHiddenResponse(
                hidden,
                changed.size(),
                artworks.stream().map(this::toHiddenResponse).toList()
        );
    }

    // 바뀌었으면 true. 이미 숨긴 작품은 처음 숨긴 시각을 유지한다
    private boolean applyHidden(Artwork artwork, boolean hidden) {
        if (artwork.isHidden() == hidden) {
            return false;
        }
        if (hidden) {
            artwork.hide(LocalDateTime.now());
        } else {
            artwork.unhide();
        }
        return true;
    }

    private NotificationType hiddenNotificationType(boolean hidden) {
        return hidden ? NotificationType.ARTWORK_HIDDEN : NotificationType.ARTWORK_SHOWN;
    }

    private ArtworkHiddenResponse toHiddenResponse(Artwork artwork) {
        return new ArtworkHiddenResponse(artwork.getId(), artwork.isHidden(), artwork.getHiddenAt());
    }

    // 작품에 연결된 작가 전원에게 알린다 (계정 없는 작가는 건너뜀)
    private void notifyArtists(Artwork artwork, NotificationType type) {
        Exhibition exhibition = artwork.getExhibition();
        Map<String, String> payload = Map.of(
                "exhibitionName", resolveExhibitionName(exhibition),
                "artworkTitle", artwork.getTitle()
        );

        for (ArtworkArtistMap map : artwork.getArtworkArtistMaps()) {
            Artist artist = map.getArtist();
            if (artist.getAccount() == null) {
                continue;
            }
            notificationService.send(artist.getAccount(), type, payload, exhibition.getId());
        }
    }

    // 전시 이름은 상세 정보의 제목을 쓰고, 없으면 slug로 대신한다
    private String resolveExhibitionName(Exhibition exhibition) {
        return exhibition.getExhibitionDetail() != null
                ? exhibition.getExhibitionDetail().getTitle()
                : exhibition.getSlug();
    }
}
