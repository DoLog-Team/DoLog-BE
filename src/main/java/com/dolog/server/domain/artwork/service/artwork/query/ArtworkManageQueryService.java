package com.dolog.server.domain.artwork.service.artwork.query;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ExhibitionFieldSettings;
import com.dolog.server.domain.artwork.entity.enums.ArtworkVisibility;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.repository.ArtworkSpecification;
import com.dolog.server.domain.artwork.repository.ExhibitionFieldSettingsRepository;
import com.dolog.server.domain.artwork.support.ArtworkFieldRequirement;
import com.dolog.server.domain.artwork.support.ArtworkValidator;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkManageItemResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkManageListResponse;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// 전시 어드민 전체/숨긴 작품 관리 목록
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtworkManageQueryService {

    private final ArtworkRepository artworkRepository;
    private final ExhibitionFieldSettingsRepository fieldSettingsRepository;
    private final ArtworkValidator artworkValidator;
    private final ArtworkFieldRequirement artworkFieldRequirement;

    public ArtworkManageListResponse getArtworks(
            UUID accountId,
            UUID exhibitionId,
            boolean hidden,
            UUID zoneId,
            boolean unassigned,
            ArtworkVisibility visibility,
            String search,
            int page,
            int size
    ) {
        Exhibition exhibition = artworkValidator.getOwnedExhibition(accountId, exhibitionId);

        if (page < 0 || size < 1 || size > 100) {
            throw new ArtworkException(ArtworkErrorCode.INVALID_PAGE_REQUEST);
        }
        if (zoneId != null && unassigned) {
            throw new ArtworkException(ArtworkErrorCode.INVALID_ZONE_FILTER);
        }
        if (zoneId != null) {
            artworkValidator.validateZone(zoneId, exhibitionId);
        }

        ExhibitionFieldSettings settings = fieldSettingsRepository.findByExhibitionId(exhibitionId)
                .orElseGet(() -> ExhibitionFieldSettings.defaultsFor(exhibition));

        Specification<Artwork> spec = Specification
                .where(ArtworkSpecification.withExhibitionId(exhibitionId))
                .and(ArtworkSpecification.withHidden(hidden))
                .and(ArtworkSpecification.withZone(zoneId, unassigned))
                .and(ArtworkSpecification.withVisibility(visibility, settings))
                .and(ArtworkSpecification.withSearch(normalize(search)))
                .and(ArtworkSpecification.orderedLikeExhibition());

        Page<Artwork> result = artworkRepository.findAll(spec, PageRequest.of(page, size));

        return new ArtworkManageListResponse(
                result.getContent().stream()
                        .map(artwork -> ArtworkManageItemResponse.of(artwork, artworkFieldRequirement.completionRate(artwork, settings)))
                        .toList(),
                (int) result.getTotalElements(),
                result.getTotalPages(),
                (int) artworkRepository.countByExhibitionIdAndHiddenAtIsNullAndPlanLimitExceededAtIsNotNull(exhibitionId)
        );
    }

    private String normalize(String search) {
        return search == null || search.isBlank() ? null : search.trim();
    }
}
