package com.dolog.server.domain.artwork.service.artwork.command;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.global.util.TextUtils;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.support.ArtworkValidator;
import com.dolog.server.domain.artwork.support.file.ArtworkFileHandler;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkCreateRequest;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtworkCreateProcessor {

    private final ArtworkRepository artworkRepository;
    private final ArtworkValidator artworkValidator;
    private final ArtworkFileHandler artworkFileHandler;

    public ArtworkCreateResponse execute(
            UUID accountId,
            ArtworkCreateRequest request
    ) {

        Artist artist = artworkValidator.getLoginArtist(accountId);

        if (request.getArtistId() != null
                && !request.getArtistId().equals(artist.getId())) {
            throw new ArtworkException(ArtworkErrorCode.ARTWORK_ARTIST_MISMATCH);
        }

        Artwork artwork = buildArtwork(request);

        if (request.getMaterials() != null) {
            artwork.replaceMaterials(request.getMaterials());
        }

        mapArtist(artwork, artist, request.getArtistRole());

        artwork.updateMainImg(
                artworkFileHandler.uploadMainImage(request.getMainImageFile())
        );

        Artwork saved = artworkRepository.save(artwork);

        return ArtworkCreateResponse.of(saved, artist.getNameKo());
    }

    // 전시/구역/순서는 출품 시점에 정해지므로 미출품(DRAFT) 상태로 만든다.
    private Artwork buildArtwork(ArtworkCreateRequest request) {

        return Artwork.builder()
                .title(request.getTitle().trim())
                .category(TextUtils.blankToNull(request.getCategory()))
                .description(TextUtils.normalizeNewlines(request.getDescription()))
                .shortIntro(TextUtils.blankToNull(request.getShortIntro()))
                .width(request.getWidth())
                .height(request.getHeight())
                .depth(request.getDepth())
                .productionStartYear(request.getProductionStartYear())
                .productionStartMonth(request.getProductionStartMonth())
                .productionStartDay(request.getProductionStartDay())
                .productionEndYear(request.getProductionEndYear())
                .productionEndMonth(request.getProductionEndMonth())
                .productionEndDay(request.getProductionEndDay())
                .purchaseUrl(TextUtils.blankToNull(request.getPurchaseUrl()))
                .purchaseChatUrl(TextUtils.blankToNull(request.getPurchaseChatUrl()))
                .showPurchaseButton(request.getShowPurchaseButton())
                .youtubeUrl(TextUtils.blankToNull(request.getYoutubeUrl()))
                .build();
    }

    private void mapArtist(
            Artwork artwork,
            Artist artist,
            String role
    ) {

        ArtworkArtistMap map =
                ArtworkArtistMap.builder()
                        .artwork(artwork)
                        .artist(artist)
                        .artistRole(role != null ? role : "")
                        .build();

        artwork.getArtworkArtistMaps().add(map);
    }
}
