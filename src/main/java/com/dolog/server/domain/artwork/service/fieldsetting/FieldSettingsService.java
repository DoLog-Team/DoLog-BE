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

        // 비공개/항목숨김 알림 문구에 "무엇이 새로 그렇게 됐는지" 넣기 위해, 바뀌기 전 값을 먼저 기억해둔다.
        boolean wasMainImgRequired = settings.isRequiredMainImg();
        boolean wasSizeRequired = settings.isRequiredSize();
        boolean wasMaterialsRequired = settings.isRequiredMaterials();
        boolean wasLocationMapRequired = settings.isRequiredLocationMap();
        boolean wasSizeHidden = settings.isHiddenSize();
        boolean wasMaterialsHidden = settings.isHiddenMaterials();
        boolean wasLocationMapHidden = settings.isHiddenLocationMap();
        boolean wasProductionPeriodHidden = settings.isHiddenProductionPeriod();
        boolean wasProductionYearHidden = settings.isHiddenProductionYear();

        FieldSettingsRequest.Required required = request.required();
        FieldSettingsRequest.Hidden hidden = request.hidden();
        settings.update(
                required.mainImg(), required.size(), required.materials(), required.locationMap(),
                hidden.size(), hidden.materials(), hidden.locationMap(),
                hidden.productionPeriod(), hidden.productionYear()
        );

        String newlyRequiredFieldNames = newlyRequiredFieldNames(
                wasMainImgRequired, wasSizeRequired, wasMaterialsRequired, wasLocationMapRequired, settings);

        List<Artwork> published = artworkRepository.findByExhibitionIdAndStatus(exhibitionId, ArtworkStatus.PUBLISHED);
        List<Artwork> drafted = artworkFieldRequirement.draftUnsatisfied(settings, published);
        drafted.forEach(artwork -> notifyArtists(
                artwork, NotificationType.ARTWORK_UNPUBLISHED, "requiredFieldNames", newlyRequiredFieldNames));

        List<Artwork> autoDraftedCandidates = artworkRepository
                .findByExhibitionIdAndStatusAndAutoDraftedAtIsNotNull(exhibitionId, ArtworkStatus.DRAFT);
        List<Artwork> republished = artworkFieldRequirement.republishAutoDrafted(settings, autoDraftedCandidates);
        republished.forEach(artwork -> notifyArtists(artwork, NotificationType.ARTWORK_REPUBLISHED, null, null));

        String newlyHiddenFieldNames = newlyHiddenFieldNames(
                wasSizeHidden, wasMaterialsHidden, wasLocationMapHidden,
                wasProductionPeriodHidden, wasProductionYearHidden, settings);
        if (!newlyHiddenFieldNames.isEmpty()) {
            artworkRepository.findByExhibitionId(exhibitionId)
                    .forEach(artwork -> notifyArtists(
                            artwork, NotificationType.ARTWORK_FIELDS_HIDDEN, "hiddenFieldNames", newlyHiddenFieldNames));
        }
    }

    // 새로 필수가 된 항목 이름을 콤마로 이어 붙인다. 하나도 없으면 빈 문자열.
    private String newlyRequiredFieldNames(
            boolean wasMainImgRequired, boolean wasSizeRequired,
            boolean wasMaterialsRequired, boolean wasLocationMapRequired,
            ExhibitionFieldSettings settings
    ) {
        List<String> names = new java.util.ArrayList<>();
        if (!wasMainImgRequired && settings.isRequiredMainImg()) names.add("대표 이미지");
        if (!wasSizeRequired && settings.isRequiredSize()) names.add("작품 사이즈");
        if (!wasMaterialsRequired && settings.isRequiredMaterials()) names.add("재료 및 기법");
        if (!wasLocationMapRequired && settings.isRequiredLocationMap()) names.add("위치 이미지");
        return String.join(", ", names);
    }

    // 새로 숨겨진 항목 이름을 콤마로 이어 붙인다. 하나도 없으면 빈 문자열.
    private String newlyHiddenFieldNames(
            boolean wasSizeHidden, boolean wasMaterialsHidden, boolean wasLocationMapHidden,
            boolean wasProductionPeriodHidden, boolean wasProductionYearHidden,
            ExhibitionFieldSettings settings
    ) {
        List<String> names = new java.util.ArrayList<>();
        if (!wasSizeHidden && settings.isHiddenSize()) names.add("작품 사이즈");
        if (!wasMaterialsHidden && settings.isHiddenMaterials()) names.add("재료 및 기법");
        if (!wasLocationMapHidden && settings.isHiddenLocationMap()) names.add("위치 이미지");
        if (!wasProductionPeriodHidden && settings.isHiddenProductionPeriod()) names.add("제작 기간");
        if (!wasProductionYearHidden && settings.isHiddenProductionYear()) names.add("제작 연도");
        return String.join(", ", names);
    }

    // 작품에 연결된 작가 전원에게 알린다 (계정 없는 작가는 건너뜀). extraKey/extraValue는 알림 종류별 부가 정보(둘 다 null이면 생략).
    private void notifyArtists(Artwork artwork, NotificationType type, String extraKey, String extraValue) {
        Exhibition exhibition = artwork.getExhibition();
        Map<String, String> payload = new java.util.HashMap<>();
        payload.put("exhibitionName", resolveExhibitionName(exhibition));
        payload.put("artworkTitle", artwork.getTitle());
        payload.put("artworkId", artwork.getId().toString());
        if (extraValue != null) {
            payload.put(extraKey, extraValue);
        }

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
