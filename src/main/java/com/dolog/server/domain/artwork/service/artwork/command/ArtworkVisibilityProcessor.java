package com.dolog.server.domain.artwork.service.artwork.command;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.support.ArtworkFieldRequirement;
import com.dolog.server.domain.artwork.support.ArtworkValidator;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkHiddenResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtworkVisibilityProcessor {

    private final ArtworkRepository artworkRepository;
    private final ArtworkValidator artworkValidator;
    private final ArtworkFieldRequirement artworkFieldRequirement;

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

        if (!hidden) {
            artwork.unhide();
        } else if (!artwork.isHidden()) {
            artwork.hide(LocalDateTime.now());
        }

        return new ArtworkHiddenResponse(artwork.getId(), artwork.isHidden(), artwork.getHiddenAt());
    }
}
