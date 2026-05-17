package com.dolog.server.domain.artwork.service.artwork.command;

import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.service.order.ArtworkOrderAdapter;
import com.dolog.server.domain.artwork.support.ArtworkValidator;
import com.dolog.server.domain.artwork.support.file.ArtworkFileHandler;
import com.dolog.server.domain.artwork.support.file.ArtworkFileUrls;
import com.dolog.server.domain.artwork.web.dto.request.ArtworkCreateRequest;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkCreateResponse;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtworkCreateProcessor {

    private final ArtworkRepository artworkRepository;
    private final ArtworkValidator artworkValidator;
    private final ArtworkFileHandler artworkFileHandler;
    private final ArtworkOrderAdapter artworkOrderAdapter;

    public ArtworkCreateResponse execute(
            ArtworkCreateRequest request
    ) {

        ArtistProfile profile =
                artworkValidator.validateArtistProfile(
                        request.getArtistProfileId()
                );

        ExhibitionZone zone =
                artworkValidator.validateZone(
                        request.getZoneId(),
                        profile.getExhibition().getId()
                );

        ArtworkFileUrls files =
                artworkFileHandler.uploadArtworkFiles(request);

        Artwork artwork =
                buildArtwork(
                        request,
                        profile,
                        zone,
                        files
                );

        artworkOrderAdapter.assign(artwork);

        mapArtist(
                artwork,
                profile,
                request.getArtistRole()
        );

        Artwork saved =
                artworkRepository.save(artwork);

        return ArtworkCreateResponse.of(
                saved,
                profile.getNameKo()
        );
    }

    private Artwork buildArtwork(
            ArtworkCreateRequest request,
            ArtistProfile profile,
            ExhibitionZone zone,
            ArtworkFileUrls files
    ) {

        return Artwork.builder()
                .exhibition(profile.getExhibition())
                .exhibitionZone(zone)
                .title(request.getTitle())
                .category(request.getCategory())
                .material(request.getMaterial())
                .size(request.getSize())
                .description(normalize(request.getDescription()))
                .mainImg(files.mainImgUrl())
                .locationMap(files.locationMapUrl())
                .youtubeUrl(request.getYoutubeUrl())
                .purchaseUrl(request.getPurchaseUrl())
                .build();
    }

    private void mapArtist(
            Artwork artwork,
            ArtistProfile profile,
            String role
    ) {

        ArtworkArtistMap map =
                ArtworkArtistMap.builder()
                        .artwork(artwork)
                        .artist(profile.getArtist())
                        .artistProfile(profile)
                        .artistRole(role != null ? role : "")
                        .build();

        artwork.getArtworkArtistMaps().add(map);
    }

    private String normalize(String description) {

        if (description == null) {
            return null;
        }

        return description.replace("\\n", "\n");
    }
}