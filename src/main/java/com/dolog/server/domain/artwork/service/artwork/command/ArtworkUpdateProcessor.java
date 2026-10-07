package com.dolog.server.domain.artwork.service.artwork.command;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.global.util.TextUtils;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.service.artist.ArtworkArtistService;
import com.dolog.server.domain.artwork.service.image.ArtworkImageService;
import com.dolog.server.domain.artwork.service.order.ArtworkOrderAdapter;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.domain.artwork.support.ArtworkFieldRequirement;
import com.dolog.server.domain.artwork.support.ArtworkValidator;
import com.dolog.server.domain.artwork.support.file.ArtworkFileHandler;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkInfoFields;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkUpdateFullRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkUpdateRequest;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkCreateResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkUpdateFullResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtworkUpdateProcessor {

    private final ArtworkRepository artworkRepository;
    private final ArtworkValidator artworkValidator;
    private final ArtworkFileHandler artworkFileHandler;
    private final ArtworkArtistService artworkArtistService;
    private final ArtworkImageService artworkImageService;
    private final ArtworkOrderAdapter artworkOrderAdapter;
    private final ArtworkFieldRequirement artworkFieldRequirement;

    public ArtworkCreateResponse update(
            UUID accountId,
            UUID artworkId,
            ArtworkUpdateRequest request
    ) {

        Artist artist = artworkValidator.getLoginArtist(accountId);
        Artwork artwork = artworkValidator.getOwnedArtwork(artworkId, artist);

        applyInfo(artwork, request);

        if (request.getArtistRole() != null) {
            artwork.getArtworkArtistMaps().stream()
                    .filter(map -> map.getArtist().getId().equals(artist.getId()))
                    .forEach(map -> map.updateRole(request.getArtistRole()));
        }

        artwork.updateMainImg(
                artworkFileHandler.replaceMainImage(artwork.getMainImg(), request.getMainImageFile())
        );

        artworkFieldRequirement.requireIfPublished(artwork);

        return ArtworkCreateResponse.of(artwork, artist.getNameKo());
    }

    // 두록 어드민의 통합 수정. path 의 전시에 출품된 작품만 다룬다.
    public ArtworkUpdateFullResponse updateFull(
            UUID exhibitionId,
            UUID artworkId,
            ArtworkUpdateFullRequest request
    ) {

        Artwork artwork = artworkRepository.findById(artworkId)
                .filter(found -> found.getExhibition() != null && found.getExhibition().getId().equals(exhibitionId))
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        applyZoneAndOrder(artwork, exhibitionId, request);

        applyInfo(artwork, request);

        artworkArtistService.syncArtistProfiles(
                artwork, exhibitionId, request.getArtistProfileIds(), request.getArtistRoles());

        if (request.getImages() != null) {
            artworkImageService.updateArtworkImages(artwork, request.getImages());
        }

        if (request.getStatus() != null) {
            artwork.changeStatus(request.getStatus());
        }
        // 공개로 바꾸거나 공개 작품의 필수 항목을 지우면 공개/비공개 API 와 같은 400
        artworkFieldRequirement.requireIfPublished(artwork);

        if (Boolean.TRUE.equals(request.getHidden()) && !artwork.isHidden()) {
            artwork.hide(LocalDateTime.now());
        } else if (Boolean.FALSE.equals(request.getHidden())) {
            artwork.unhide();
        }

        artworkRepository.saveAndFlush(artwork);

        return ArtworkUpdateFullResponse.from(artwork);
    }

    // 구역은 그 전시 것만. 구역을 옮기면서 순서를 안 주면 새 구역의 마지막으로 보낸다.
    private void applyZoneAndOrder(Artwork artwork, UUID exhibitionId, ArtworkUpdateFullRequest request) {

        Integer prev = request.getPrevOrder();
        Integer next = request.getNextOrder();
        if ((prev == null) != (next == null)) {
            throw new ArtworkException(ArtworkErrorCode.INVALID_ORDER_REQUEST);
        }

        ExhibitionZone zone = artworkValidator.validateZone(request.getZoneId(), exhibitionId);
        boolean zoneChanged = artwork.getExhibitionZone() == null
                || !artwork.getExhibitionZone().getId().equals(zone.getId());

        if (zoneChanged) {
            artwork.updateZone(zone);
        }

        if (prev != null) {
            artworkOrderAdapter.reorder(artwork, prev, next, zone.getId());
        } else if (zoneChanged) {
            artworkOrderAdapter.assign(artwork);
        }
    }

    // 안 보낸 값(null)은 기존 값을 유지한다.
    private void applyInfo(Artwork artwork, ArtworkInfoFields request) {

        artwork.updateText(
                request.getTitle() != null ? request.getTitle().trim() : null,
                request.getCategory(),
                normalize(request.getDescription()),
                request.getShortIntro()
        );
        artwork.updateSize(request.getWidth(), request.getHeight(), request.getDepth());
        artwork.updateProductionPeriod(
                request.getProductionStartYear(),
                request.getProductionStartMonth(),
                request.getProductionStartDay(),
                request.getProductionEndYear(),
                request.getProductionEndMonth(),
                request.getProductionEndDay()
        );
        artwork.updatePurchaseInfo(
                request.getPurchaseUrl(),
                request.getPurchaseChatUrl(),
                request.getShowPurchaseButton(),
                request.getYoutubeUrl()
        );
        if (request.getMaterials() != null) {
            artwork.replaceMaterials(request.getMaterials());
        }
    }

    private String normalize(String description) {
        return TextUtils.normalizeNewlines(description);
    }
}