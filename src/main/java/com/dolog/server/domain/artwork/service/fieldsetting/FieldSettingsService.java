package com.dolog.server.domain.artwork.service.fieldsetting;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.entity.ExhibitionFieldSettings;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.repository.ExhibitionFieldSettingsRepository;
import com.dolog.server.domain.artwork.support.ArtworkFieldRequirement;
import com.dolog.server.domain.artwork.web.dto.request.FieldSettingsRequest;
import com.dolog.server.domain.artwork.web.dto.response.FieldSettingsResponse;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
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
public class FieldSettingsService {

    private final ExhibitionRepository exhibitionRepository;
    private final ExhibitionFieldSettingsRepository fieldSettingsRepository;
    private final ArtworkRepository artworkRepository;
    private final ArtworkFieldRequirement artworkFieldRequirement;
    private final NotificationService notificationService;

    // 설정 행이 없는 전시는 모든 항목이 선택/노출인 기본값으로 응답한다 (행은 만들지 않음).
    @Transactional(readOnly = true)
    public FieldSettingsResponse get(UUID accountId, UUID exhibitionId) {

        Exhibition exhibition = getOwnedExhibition(accountId, exhibitionId);

        return FieldSettingsResponse.from(
                fieldSettingsRepository.findByExhibitionId(exhibitionId)
                        .orElseGet(() -> ExhibitionFieldSettings.defaultsFor(exhibition))
        );
    }

    // 저장 후 필수 항목을 못 채운 공개 작품은 비공개로 내린다 (명세: 필수값 강화 시 자동 DRAFT).
    public void update(UUID accountId, UUID exhibitionId, FieldSettingsRequest request) {

        Exhibition exhibition = getOwnedExhibition(accountId, exhibitionId);

        if (request.hasRequiredAndHiddenConflict()) {
            throw new ArtworkException(ArtworkErrorCode.REQUIRED_FIELD_HIDDEN);
        }

        ExhibitionFieldSettings settings = fieldSettingsRepository.findByExhibitionId(exhibitionId)
                .orElseGet(() -> fieldSettingsRepository.save(ExhibitionFieldSettings.defaultsFor(exhibition)));

        FieldSettingsRequest.Required required = request.required();
        FieldSettingsRequest.Hidden hidden = request.hidden();
        settings.update(
                required.mainImg(), required.size(), required.materials(), required.locationMap(),
                hidden.size(), hidden.materials(), hidden.locationMap(),
                hidden.productionPeriod(), hidden.productionYear()
        );

        List<Artwork> published = artworkRepository.findByExhibitionIdAndStatus(exhibitionId, ArtworkStatus.PUBLISHED);
        List<Artwork> drafted = artworkFieldRequirement.draftUnsatisfied(settings, published);
        drafted.forEach(artwork -> notifyArtists(artwork, NotificationType.ARTWORK_UNPUBLISHED));

        List<Artwork> autoDraftedCandidates = artworkRepository
                .findByExhibitionIdAndStatusAndAutoDraftedAtIsNotNull(exhibitionId, ArtworkStatus.DRAFT);
        List<Artwork> republished = artworkFieldRequirement.republishAutoDrafted(settings, autoDraftedCandidates);
        republished.forEach(artwork -> notifyArtists(artwork, NotificationType.ARTWORK_REPUBLISHED));
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

    private Exhibition getOwnedExhibition(UUID accountId, UUID exhibitionId) {

        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        if (!exhibition.getAccount().getId().equals(accountId)) {
            throw new ArtworkException(ArtworkErrorCode.NOT_EXHIBITION_OWNER);
        }

        return exhibition;
    }
}
