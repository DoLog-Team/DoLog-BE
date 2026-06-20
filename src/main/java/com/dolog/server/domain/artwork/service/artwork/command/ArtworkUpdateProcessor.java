package com.dolog.server.domain.artwork.service.artwork.command;

import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.service.artist.ArtworkArtistService;
import com.dolog.server.domain.artwork.service.image.ArtworkImageService;
import com.dolog.server.domain.artwork.support.ArtworkOrderHandler;
import com.dolog.server.domain.artwork.support.ArtworkValidator;
import com.dolog.server.domain.artwork.support.file.ArtworkFileHandler;
import com.dolog.server.domain.artwork.support.file.ArtworkFileUrls;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkUpdateFullRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkUpdateRequest;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkCreateResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkUpdateFullResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ArtworkOrderHandler artworkOrderHandler;

    public ArtworkCreateResponse update(
            UUID artworkId,
            ArtworkUpdateRequest request
    ) {

        Artwork artwork = getArtwork(artworkId);

        artworkOrderHandler.apply(
                artwork,
                request.getPrevOrder(),
                request.getNextOrder(),
                request.getZoneId()
        );

        ArtworkFileUrls files =
                artworkFileHandler.updateArtworkFiles(
                        artwork,
                        request
                );

        String artistName =
                updateArtistProfile(
                        artwork,
                        request
                );

        artwork.updateAllInfo(
                request.getTitle(),
                normalize(request.getDescription()),
                request.getCategory(),
                artwork.getExhibitionZone(),
                request.getMaterial(),
                request.getSize(),
                files.mainImgUrl(),
                files.locationMapUrl(),
                request.getPurchaseUrl(),
                request.getYoutubeUrl()
        );

        return ArtworkCreateResponse.of(
                artwork,
                artistName
        );
    }

    public ArtworkUpdateFullResponse updateFull(
            UUID artworkId,
            ArtworkUpdateFullRequest request
    ) {

        Artwork artwork = getArtwork(artworkId);

        artworkOrderHandler.apply(
                artwork,
                request.getPrevOrder(),
                request.getNextOrder(),
                request.getZoneId()
        );

        artwork.updateAllInfo(
                request.getTitle(),
                normalize(request.getDescription()),
                request.getCategory(),
                artwork.getExhibitionZone(),
                artwork.getMaterial(),
                artwork.getSize(),
                artwork.getMainImg(),
                artwork.getLocationMap(),
                request.getPurchaseUrl(),
                request.getYoutubeUrl()
        );

        artworkArtistService.updateArtworkArtists(
                artwork,
                request.getArtistIds(),
                request.getArtistRoles()
        );

        artworkImageService.updateArtworkImages(
                artwork,
                request.getImages()
        );

        artworkRepository.saveAndFlush(artwork);

        return ArtworkUpdateFullResponse.of(
                artwork.getId(),
                artwork.getTitle(),
                artwork.getArtworkArtistMaps()
                        .stream()
                        .map(map -> map.getArtist().getId())
                        .toList(),
                artwork.getArtworkImg()
                        .stream()
                        .map(img -> img.getId())
                        .toList()
        );
    }

    private Artwork getArtwork(UUID artworkId) {

        return artworkRepository.findById(artworkId)
                .orElseThrow(() ->
                        new ArtworkException(
                                ArtworkErrorCode.ARTWORK_NOT_FOUND
                        )
                );
    }

    private String updateArtistProfile(
            Artwork artwork,
            ArtworkUpdateRequest request
    ) {

        if (request.getArtistProfileId() == null) {

            return artwork.getArtworkArtistMaps()
                    .stream()
                    .findFirst()
                    .map(map ->
                            map.getArtistProfile() != null
                                    ? map.getArtistProfile().getNameKo()
                                    : map.getArtist().getNameKo()
                    )
                    .orElse("Unknown Artist");
        }

        ArtistProfile profile =
                artworkValidator.validateArtistProfile(
                        request.getArtistProfileId()
                );

        artwork.getArtworkArtistMaps()
                .get(0)
                .updateArtistProfile(
                        profile.getArtist(),
                        profile,
                        request.getArtistRole()
                );

        return profile.getNameKo();
    }

    private String normalize(String description) {

        if (description == null) {
            return null;
        }

        return description.replace("\\n", "\n");
    }
}