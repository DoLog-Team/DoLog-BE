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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// "출품된 PUBLISHED 작품은 그 전시의 필수 항목을 채운다" 규칙을 한곳에서 판단한다.
@Component
@RequiredArgsConstructor
public class ArtworkFieldRequirement {

    private final ExhibitionFieldSettingsRepository fieldSettingsRepository;
    private final ExhibitionZoneRepository exhibitionZoneRepository;

    public boolean isSatisfied(Artwork artwork) {
        Context context = resolveContext(artwork);
        return requirementChecks(artwork, context.settings(), context.exhibitionHasZones())
                .stream().allMatch(Boolean::booleanValue);
    }

    // 작품 작성 완료율(%). 작품 기본/상세 필수값 + 그룹(구역이 있는 전시인 경우) + 출품 정보 필수값(전시에 출품한 경우)을 합산한다.
    public int completionRate(Artwork artwork) {
        Context context = resolveContext(artwork);
        return completionRate(artwork, context.settings(), context.exhibitionHasZones());
    }

    // 목록 조회처럼 전시별 설정/구역 존재 여부를 미리 한 번만 조회해둔 경우, 매 작품마다 다시 조회하지 않도록 직접 받는다.
    public int completionRate(Artwork artwork, ExhibitionFieldSettings settings, boolean exhibitionHasZones) {
        List<Boolean> checks = requirementChecks(artwork, settings, exhibitionHasZones);
        long filled = checks.stream().filter(Boolean::booleanValue).count();
        return Math.round(filled * 100f / checks.size());
    }

    // 이 작품에 적용되는 "충족 여부" 체크 목록. isSatisfied/completionRate 둘 다 이 목록만 본다 —
    // 필수 항목이 새로 생기면 여기 한 줄만 추가하면 둘 다 자동으로 반영된다.
    private List<Boolean> requirementChecks(Artwork artwork, ExhibitionFieldSettings settings, boolean exhibitionHasZones) {
        List<Boolean> checks = new ArrayList<>();
        checks.add(!isBlank(artwork.getCategory()));
        checks.add(artwork.getProductionEndYear() != null);

        // 그룹이 하나도 없는 전시는 작가가 고를 UI 자체가 없으므로 체크하지 않는다.
        if (exhibitionHasZones) {
            checks.add(artwork.getExhibitionZone() != null);
        }

        if (settings != null) {
            if (settings.isRequiredMainImg()) checks.add(!isBlank(artwork.getMainImg()));
            // 사이즈는 가로/세로만 본다. 높이는 부피가 있는 작품만 입력한다.
            if (settings.isRequiredSize()) checks.add(artwork.getWidth() != null && artwork.getHeight() != null);
            if (settings.isRequiredMaterials()) checks.add(!artwork.getMaterials().isEmpty());
            if (settings.isRequiredLocationMap()) checks.add(!isBlank(artwork.getLocationMap()));
        }

        return checks;
    }

    private Context resolveContext(Artwork artwork) {
        if (artwork.getExhibition() == null) {
            return new Context(null, false);
        }
        UUID exhibitionId = artwork.getExhibition().getId();
        return new Context(
                fieldSettingsRepository.findByExhibitionId(exhibitionId).orElse(null),
                exhibitionZoneRepository.existsByExhibitionId(exhibitionId)
        );
    }

    private record Context(ExhibitionFieldSettings settings, boolean exhibitionHasZones) {}

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
