package com.dolog.server.domain.artwork.service.artwork.query.mapper;

import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkCreateResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkDetailResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkListResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ArtworkResponseMapper {

    /*
     * =========================================================
     * Artwork List
     * =========================================================
     */

    public List<ArtworkListResponse> toArtworkListResponses(
            List<Artwork> artworks,
            Map<UUID, String> artistMap,
            Map<UUID, String> exhibitionDetailMap
    ) {

        return artworks.stream()
                .map(artwork ->
                        toArtworkListResponse(
                                artwork,
                                artistMap,
                                exhibitionDetailMap
                        )
                )
                .toList();
    }

    private ArtworkListResponse toArtworkListResponse(
            Artwork artwork,
            Map<UUID, String> artistMap,
            Map<UUID, String> exhibitionDetailMap
    ) {

        UUID exhibitionId = artwork.getExhibition().getId();

        return ArtworkListResponse.builder()
                .id(artwork.getId())
                .title(artwork.getTitle())
                .artistName(
                        artistMap.get(artwork.getId())
                )
                .category(artwork.getCategory())
                .imageUrl(artwork.getMainImg())

                .exhibitionId(exhibitionId)
                .exhibitionTitle(exhibitionDetailMap.get(exhibitionId))
                .deptName(artwork.getExhibition().getDeptName())
                .slug(artwork.getExhibition().getSlug())

                .zoneId(
                        artwork.getExhibitionZone() != null
                                ? artwork.getExhibitionZone().getId()
                                : null
                )

                .zoneName(
                        artwork.getExhibitionZone() != null
                                ? artwork.getExhibitionZone().getName()
                                : null
                )

                .orderIndex(artwork.getOrderIndex())
                .build();
    }

    /*
     * =========================================================
     * Artwork Create
     * =========================================================
     */

    public ArtworkCreateResponse toArtworkCreateResponse(
            Artwork artwork,
            String artistName
    ) {

        return ArtworkCreateResponse.of(
                artwork,
                artistName
        );
    }

    /*
     * =========================================================
     * Artwork Detail
     * =========================================================
     */

    public ArtworkDetailResponse.RelatedArtworkInfo toRelatedArtworkInfo(
            Artwork artwork,
            String type
    ) {

        return ArtworkDetailResponse.RelatedArtworkInfo.builder()
                .id(artwork.getId())
                .title(artwork.getTitle())
                .category(artwork.getCategory())
                .artistName(getArtistNames(artwork))
                .mainImage(artwork.getMainImg())
                .type(type)
                .build();
    }

    public ArtworkDetailResponse.ParticipantInfo toParticipantInfo(
            ArtworkArtistMap map
    ) {

        ArtistProfile profile = map.getArtistProfile();

        return ArtworkDetailResponse.ParticipantInfo.builder()
                .artistId(map.getArtist().getId())

                .profileId(
                        profile != null
                                ? profile.getId()
                                : null
                )

                .nameKo(
                        profile != null
                                ? profile.getNameKo()
                                : map.getArtist().getNameKo()
                )

                .nameEn(
                        profile != null
                                ? profile.getNameEn()
                                : map.getArtist().getNameEn()
                )

                .profileImg(
                        profile != null
                                ? profile.getProfileImg()
                                : null
                )

                .role(map.getArtistRole())

                .bio(
                        profile != null
                                ? profile.getBio()
                                : null
                )

                .email(
                        profile != null
                                ? profile.getEmail()
                                : null
                )

                .sns(
                        profile != null
                                ? profile.getSnsList().stream()
                                .map(sns ->
                                        ArtworkDetailResponse.SnsInfo.builder()
                                                .platformName(sns.getPlatformName())
                                                .url(sns.getUrl())
                                                .build()
                                )
                                .toList()
                                : Collections.emptyList()
                )

                .build();
    }

    /*
     * =========================================================
     * Common Helper
     * =========================================================
     */

    private String getArtistNames(Artwork artwork) {

        return artwork.getArtworkArtistMaps().stream()
                .map(map -> {

                    if (
                            map.getArtistProfile() != null &&
                                    map.getArtistProfile().getNameKo() != null
                    ) {
                        return map.getArtistProfile().getNameKo();
                    }

                    return map.getArtist().getNameKo();
                })
                .collect(Collectors.joining(", "));
    }
}