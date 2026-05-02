package com.dolog.server.domain.artwork.service.artworkUpdate;

import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkImg;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.service.image.ArtworkImageService;
import com.dolog.server.domain.artwork.service.artist.ArtworkArtistService;
import com.dolog.server.domain.artwork.service.order.ArtworkOrderAdapter;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkUpdateFullRequest;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkUpdateRequest;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkCreateResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkUpdateFullResponse;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionZoneRepository;
import com.dolog.server.global.util.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ArtworkUpdateServiceImpl implements ArtworkUpdateService {
    private final ArtworkRepository artworkRepository;
    private final ExhibitionZoneRepository exhibitionZoneRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final FileService fileService;
    private final ArtworkImageService artworkImageService;
    private final ArtworkArtistService artworkArtistService;
    private final ArtworkOrderAdapter artworkOrderAdapter;

    @Override
    public ArtworkCreateResponse updateArtwork(UUID artworkId, ArtworkUpdateRequest request) {

        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        applyOrderAndZone(
                artwork,
                request.getPrevOrder(),
                request.getNextOrder(),
                request.getZoneId()
        );

        // 파일 처리
        String mainImgUrl = artwork.getMainImg();
        String locationMapUrl = artwork.getLocationMap();

        try {
            if (request.getMainImageFile() != null && !request.getMainImageFile().isEmpty()) {
                fileService.deleteFile(artwork.getMainImg());
                mainImgUrl = fileService.uploadFile(request.getMainImageFile(), "artworks/main");
            }

            if (request.getLocationMapFile() != null && !request.getLocationMapFile().isEmpty()) {
                fileService.deleteFile(artwork.getLocationMap());
                locationMapUrl = fileService.uploadFile(request.getLocationMapFile(), "artworks/maps");
            }
        } catch (IOException e) {
            throw new RuntimeException("파일 수정 중 오류 발생");
        }

        String artistName = handleArtistProfile(artwork, request);

        artwork.updateAllInfo(
                request.getTitle(),
                request.getDescription(),
                request.getCategory(),
                null,
                request.getMaterial(),
                request.getSize(),
                mainImgUrl,
                locationMapUrl,
                request.getPurchaseUrl(),
                request.getYoutubeUrl()
        );

        return ArtworkCreateResponse.of(artwork, artistName);
    }


    @Override
    public ArtworkUpdateFullResponse updateArtworkFull(UUID exhibitionId, UUID artworkId, ArtworkUpdateFullRequest request) {

        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        applyOrderAndZone(
                artwork,
                request.getPrevOrder(),
                request.getNextOrder(),
                request.getZoneId()
        );

        artwork.updateAllInfo(
                request.getTitle(),
                request.getDescription(),
                request.getCategory(),
                artwork.getExhibitionZone(),
                artwork.getMaterial(),
                artwork.getSize(),
                artwork.getMainImg(),
                artwork.getLocationMap(),
                request.getPurchaseUrl(),
                request.getYoutubeUrl()
        );

        artworkArtistService.updateArtworkArtists(artwork, request.getArtistIds());
        artworkImageService.updateArtworkImages(artwork, request.getImages());

        artworkRepository.saveAndFlush(artwork);

        return ArtworkUpdateFullResponse.of(
                artwork.getId(),
                artwork.getTitle(),
                artwork.getArtworkArtistMaps().stream().map(m -> m.getArtist().getId()).toList(),
                artwork.getArtworkImg().stream().map(i -> i.getId()).toList()
        );
    }

    private void applyOrderAndZone(Artwork artwork, Integer prev, Integer next, UUID zoneId) {

        if ((prev == null && next != null) || (prev != null && next == null)) {
            throw new ArtworkException(ArtworkErrorCode.INVALID_ORDER_REQUEST);
        }

        if (zoneId != null) {
            artworkOrderAdapter.moveZone(artwork, zoneId, prev, next);
        } else if (prev != null && next != null) {
            artworkOrderAdapter.reorder(artwork, prev, next);
        }
    }

    private String handleArtistProfile(Artwork artwork, ArtworkUpdateRequest request) {
        if (request.getArtistProfileId() != null) {
            ArtistProfile newProfile = artistProfileRepository.findById(request.getArtistProfileId())
                    .orElseThrow(() -> new RuntimeException("Artist Profile not found"));

            if (!artwork.getArtworkArtistMaps().isEmpty()) {
                artwork.getArtworkArtistMaps().get(0)
                        .updateArtistProfile(newProfile.getArtist(), newProfile, request.getArtistRole());
            }
            return newProfile.getNameKo();
        }

        return artwork.getArtworkArtistMaps().stream()
                .findFirst()
                .map(m -> m.getArtistProfile() != null ? m.getArtistProfile().getNameKo() : m.getArtist().getNameKo())
                .orElse("Unknown Artist");
    }

}
