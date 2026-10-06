package com.dolog.server.domain.artwork.support;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ExhibitionFieldSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// "출품된 PUBLISHED 작품은 그 전시의 필수 항목을 채운다" 규칙을 한곳에서 판단한다.
@Component
@RequiredArgsConstructor
public class ArtworkFieldRequirement {

    private final ExhibitionFieldSettingsRepository fieldSettingsRepository;

    public boolean isSatisfied(Artwork artwork) {

        if (artwork.getExhibition() == null) {
            return true;
        }

        return fieldSettingsRepository.findByExhibitionId(artwork.getExhibition().getId())
                .map(settings -> settings.isSatisfiedBy(artwork))
                .orElse(true);
    }

    // 작가가 직접 공개하거나 수정할 때: 규칙을 깨면 거절한다.
    public void requireIfPublished(Artwork artwork) {

        if (artwork.getStatus() == ArtworkStatus.PUBLISHED && !isSatisfied(artwork)) {
            throw new ArtworkException(ArtworkErrorCode.REQUIRED_FIELDS_MISSING);
        }
    }

    // 전시 쪽 기준이 새로 걸릴 때(출품 등): 규칙을 못 채우면 비공개로 내린다.
    public void draftIfUnsatisfied(Artwork artwork) {

        if (artwork.getStatus() == ArtworkStatus.PUBLISHED && !isSatisfied(artwork)) {
            artwork.changeStatus(ArtworkStatus.DRAFT);
        }
    }
}
