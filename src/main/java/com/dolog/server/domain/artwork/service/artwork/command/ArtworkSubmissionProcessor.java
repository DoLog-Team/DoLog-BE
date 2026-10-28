package com.dolog.server.domain.artwork.service.artwork.command;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.entity.ExhibitionFieldSettings;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ExhibitionFieldSettingsRepository;
import com.dolog.server.domain.artwork.service.order.ArtworkOrderAdapter;
import com.dolog.server.domain.artwork.support.ArtworkFieldRequirement;
import com.dolog.server.domain.artwork.support.ArtworkSubmissionCanceller;
import com.dolog.server.domain.artwork.support.ArtworkValidator;
import com.dolog.server.domain.artwork.support.file.ArtworkFileHandler;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkSubmitRequest;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkExhibitionStatusResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkSubmitResponse;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionArtistMap;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.domain.exhibition.entity.enums.ExhibitionArtistStatus;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.domain.notification.entity.enums.NotificationType;
import com.dolog.server.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtworkSubmissionProcessor {

    private final ArtworkValidator artworkValidator;
    private final ArtworkFileHandler artworkFileHandler;
    private final ArtworkOrderAdapter artworkOrderAdapter;
    private final ExhibitionArtistMapRepository exhibitionArtistMapRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final ArtworkFieldRequirement artworkFieldRequirement;
    private final ArtworkSubmissionCanceller artworkSubmissionCanceller;
    private final NotificationService notificationService;
    private final ArtworkPlanLimitService artworkPlanLimitService;
    private final ExhibitionFieldSettingsRepository fieldSettingsRepository;

    public ArtworkSubmitResponse submit(
            UUID accountId,
            UUID artworkId,
            ArtworkSubmitRequest request
    ) {

        Artist artist = artworkValidator.getLoginArtist(accountId);
        Artwork artwork = artworkValidator.getOwnedArtwork(artworkId, artist);

        if (artwork.getExhibition() != null) {
            throw new ArtworkException(ArtworkErrorCode.ARTWORK_ALREADY_SUBMITTED);
        }

        Exhibition exhibition = getJoinedExhibition(request.getExhibitionId(), artist);
        ExhibitionZone zone = request.getZoneId() != null
                ? artworkValidator.validateZone(request.getZoneId(), exhibition.getId())
                : null;

        // 출품하는 작가는 공통 프로필이 꼭 있어야 하고,
        // 다른 연결 작가는 해당 전시에 참여 중일 때 공통 프로필을 연결한다.
        ArtistProfile myProfile = artistProfileRepository.findByArtistId(artist.getId())
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTIST_NOT_JOINED_EXHIBITION));

        for (ArtworkArtistMap map : artwork.getArtworkArtistMaps()) {
            if (map.getArtist().getId().equals(artist.getId())) {
                map.linkProfile(myProfile);
            } else if (exhibitionArtistMapRepository
                    .existsByExhibitionIdAndArtistIdAndStatus(
                            exhibition.getId(),
                            map.getArtist().getId(),
                            ExhibitionArtistStatus.JOINED
                    )) {
                artistProfileRepository.findByArtistId(map.getArtist().getId())
                        .ifPresent(map::linkProfile);
            }
        }

        artwork.submitTo(exhibition, zone);
        artwork.updateLocationMap(
                artworkFileHandler.uploadLocationMap(request.getLocationMapFile())
        );
        if (zone != null) {
            artworkOrderAdapter.assign(artwork);
        }
        artworkFieldRequirement.draftIfUnsatisfied(artwork);
        artworkPlanLimitService.recompute(exhibition.getId());

        return ArtworkSubmitResponse.from(artwork);
    }

    // 작가 본인의 "내 작품이 지금 어느 전시/구역에 출품돼 있고 숨김 상태인지" 조회
    public ArtworkExhibitionStatusResponse getSubmissionStatus(
            UUID accountId,
            UUID artworkId
    ) {
        Artist artist = artworkValidator.getLoginArtist(accountId);
        Artwork artwork = artworkValidator.getOwnedArtwork(artworkId, artist);

        if (artwork.getExhibition() == null) {
            throw new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_SUBMITTED);
        }

        Exhibition exhibition = artwork.getExhibition();
        ExhibitionFieldSettings settings = fieldSettingsRepository.findByExhibitionId(exhibition.getId())
                .orElseGet(() -> ExhibitionFieldSettings.defaultsFor(exhibition));

        return ArtworkExhibitionStatusResponse.of(artwork, resolveExhibitionName(exhibition), settings);
    }

    public void cancel(
            UUID accountId,
            UUID artworkId
    ) {

        Artist artist = artworkValidator.getLoginArtist(accountId);
        Artwork artwork = artworkValidator.getOwnedArtwork(artworkId, artist);

        Exhibition exhibition = artwork.getExhibition();
        if (exhibition == null) {
            throw new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_SUBMITTED);
        }
        String artworkTitle = artwork.getTitle();

        artworkSubmissionCanceller.cancel(artwork);
        artworkPlanLimitService.recompute(exhibition.getId());

        notificationService.send(
                exhibition.getAccount(),
                NotificationType.ARTWORK_CANCELLED,
                Map.of(
                        "artistName", java.util.Objects.toString(artist.getProfileNameKo(), "이름 미등록"),
                        "artworkTitle", artworkTitle,
                        "exhibitionName", resolveExhibitionName(exhibition)
                ),
                exhibition.getId()
        );
    }

    // 전시 이름은 상세 정보의 제목을 쓰고, 없으면 slug로 대신한다
    private String resolveExhibitionName(Exhibition exhibition) {
        return exhibition.getExhibitionDetail() != null
                ? exhibition.getExhibitionDetail().getTitle()
                : exhibition.getSlug();
    }

    private Exhibition getJoinedExhibition(UUID exhibitionId, Artist artist) {

        return exhibitionArtistMapRepository.findByExhibitionIdAndArtistId(exhibitionId, artist.getId())
                .filter(map -> map.getStatus() == ExhibitionArtistStatus.JOINED)
                .map(ExhibitionArtistMap::getExhibition)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTIST_NOT_JOINED_EXHIBITION));
    }
}
