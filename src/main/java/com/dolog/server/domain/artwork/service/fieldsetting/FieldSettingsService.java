package com.dolog.server.domain.artwork.service.fieldsetting;

import com.dolog.server.domain.artwork.entity.Artwork;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class FieldSettingsService {

    private final ExhibitionRepository exhibitionRepository;
    private final ExhibitionFieldSettingsRepository fieldSettingsRepository;
    private final ArtworkRepository artworkRepository;
    private final ArtworkFieldRequirement artworkFieldRequirement;

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
        artworkFieldRequirement.draftUnsatisfied(settings, published);
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
