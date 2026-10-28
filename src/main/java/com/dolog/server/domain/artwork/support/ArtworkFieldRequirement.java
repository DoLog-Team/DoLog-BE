package com.dolog.server.domain.artwork.support;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ExhibitionFieldSettings;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ExhibitionFieldSettingsRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

// "출품된 PUBLISHED 작품은 그 전시의 필수 항목을 채운다" 규칙을 한곳에서 판단한다.
@Component
@RequiredArgsConstructor
public class ArtworkFieldRequirement {

    private final ExhibitionFieldSettingsRepository fieldSettingsRepository;
    private final ExhibitionZoneRepository exhibitionZoneRepository;

    public boolean isSatisfied(Artwork artwork) {

        if (isBlank(artwork.getCategory()) || artwork.getProductionEndYear() == null) {
            return false;
        }

        if (artwork.getExhibition() == null) {
            return true;
        }

        // 그룹이 하나도 없는 전시는 작가가 고를 UI 자체가 없으므로 체크하지 않는다.
        if (artwork.getExhibitionZone() == null
                && exhibitionZoneRepository.existsByExhibitionId(artwork.getExhibition().getId())) {
            return false;
        }

        return fieldSettingsRepository.findByExhibitionId(artwork.getExhibition().getId())
                .map(settings -> settings.isSatisfiedBy(artwork))
                .orElse(true);
    }

    // 작품 기본/상세 쪽 고정 필수값 (전시 설정과 무관하게 항상 본다)
    private static final int BASE_REQUIRED_COUNT = 2; // category, productionEndYear

    // 작품 작성 완료율(%). 작품 기본/상세 필수값 + 출품 정보 필수값(전시에 출품한 경우)을 합산한다.
    public int completionRate(Artwork artwork) {

        ExhibitionFieldSettings settings = artwork.getExhibition() != null
                ? fieldSettingsRepository.findByExhibitionId(artwork.getExhibition().getId()).orElse(null)
                : null;

        return completionRate(artwork, settings);
    }

    // 목록 조회처럼 전시별 설정을 미리 한 번만 조회해둔 경우, 매 작품마다 다시 조회하지 않도록 settings 를 직접 받는다.
    public int completionRate(Artwork artwork, ExhibitionFieldSettings settings) {

        int required = BASE_REQUIRED_COUNT;
        int filled = baseFilledCount(artwork);

        if (settings != null) {
            required += settings.requiredFieldCount();
            filled += settings.filledRequiredFieldCount(artwork);
        }

        return Math.round(filled * 100f / required);
    }

    private int baseFilledCount(Artwork artwork) {
        int filled = 0;
        if (!isBlank(artwork.getCategory())) filled++;
        if (artwork.getProductionEndYear() != null) filled++;
        return filled;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // 작가가 직접 공개하거나 수정할 때: 규칙을 깨면 거절한다.
    public void requireIfPublished(Artwork artwork) {

        if (artwork.getStatus() == ArtworkStatus.PUBLISHED && !isSatisfied(artwork)) {
            throw new ArtworkException(ArtworkErrorCode.REQUIRED_FIELDS_MISSING);
        }
    }

    // 전시의 필수 항목이 바뀔 때: 그 전시의 공개 작품 중 못 채운 작품을 자동으로 비공개로 내리고 돌려준다.
    public List<Artwork> draftUnsatisfied(ExhibitionFieldSettings settings, List<Artwork> publishedArtworks) {

        List<Artwork> drafted = publishedArtworks.stream()
                .filter(artwork -> !settings.isSatisfiedBy(artwork))
                .toList();

        LocalDateTime now = LocalDateTime.now();
        drafted.forEach(artwork -> artwork.autoDraft(now));
        return drafted;
    }

    // 전시 쪽 기준이 새로 걸릴 때(출품 등): 규칙을 못 채우면 자동으로 비공개로 내린다.
    public void draftIfUnsatisfied(Artwork artwork) {

        if (artwork.getStatus() == ArtworkStatus.PUBLISHED && !isSatisfied(artwork)) {
            artwork.autoDraft(LocalDateTime.now());
        }
    }

    // 전시의 필수 항목이 완화될 때: 시스템이 자동으로 비공개했던 작품 중 이제 조건을 채운 작품을 다시 공개하고 돌려준다.
    // 작가가 직접 비공개로 둔 작품(autoDraftedAt == null)은 건드리지 않는다.
    public List<Artwork> republishAutoDrafted(ExhibitionFieldSettings settings, List<Artwork> autoDraftedArtworks) {

        List<Artwork> republished = autoDraftedArtworks.stream()
                .filter(artwork -> settings.isSatisfiedBy(artwork))
                .toList();

        republished.forEach(Artwork::autoPublish);
        return republished;
    }
}
