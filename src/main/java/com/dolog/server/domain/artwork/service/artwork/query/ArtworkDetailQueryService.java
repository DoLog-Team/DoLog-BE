package com.dolog.server.domain.artwork.service.artwork.query;

import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkDetailResponse;
import com.dolog.server.domain.bts.repository.BtsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtworkDetailQueryService {

    private final ArtworkRepository artworkRepository;
    private final BtsRepository btsRepository;

    public ArtworkDetailResponse getArtworkDetail(
            UUID exhibitionId,
            UUID artworkId
    ) {

        Artwork artwork = artworkRepository.findDetailById(
                        exhibitionId,
                        artworkId
                )
                .orElseThrow(() ->
                        new ArtworkException(
                                ArtworkErrorCode.ARTWORK_NOT_FOUND
                        )
                );

        List<ArtworkDetailResponse.RelatedBtsInfo> relatedBts =
                getRelatedBts(artworkId);

        List<ArtworkDetailResponse.RelatedArtworkInfo> relatedArtworks =
                getRelatedArtworks(exhibitionId, artwork);

        List<ArtworkDetailResponse.RelatedArtworkInfo> navigationArtworks =
                getNavigationArtworks(exhibitionId, artwork);

        return ArtworkDetailResponse.builder()
                .title(artwork.getTitle())
                .category(artwork.getCategory())
                .material(artwork.getMaterial())
                .size(artwork.getSize())
                .description(artwork.getDescription())
                .youtubeUrl(artwork.getYoutubeUrl())
                .purchaseUrl(artwork.getPurchaseUrl())
                .mainImage(artwork.getMainImg())
                .locationMap(artwork.getLocationMap())
                .detailImages(
                        artwork.getArtworkImg().stream()
                                .map(img ->
                                        ArtworkDetailResponse.DetailImageInfo.builder()
                                                .imageId(img.getId())
                                                .imageUrl(img.getImageUrl())
                                                .description(img.getDescription())
                                                .build()
                                )
                                .toList()
                )
                .participants(
                        artwork.getArtworkArtistMaps().stream()
                                .map(this::convertToParticipantInfo)
                                .toList()
                )
                .relatedBts(relatedBts)
                .sameCategoryArtworks(relatedArtworks)
                .alphabeticalArtworks(navigationArtworks)
                .build();
    }

    private List<ArtworkDetailResponse.RelatedBtsInfo>
    getRelatedBts(UUID artworkId) {

        return btsRepository.findAllByArtworkId(artworkId)
                .stream()
                .map(bts -> {

                    String authorName =
                            bts.getArtistProfile() != null
                                    ? bts.getArtistProfile().getNameKo()
                                    : "작가 미상";

                    return ArtworkDetailResponse.RelatedBtsInfo.builder()
                            .id(bts.getId())
                            .title(bts.getTitle())
                            .mainImg(bts.getMainImg())
                            .author(authorName)
                            .build();
                })
                .toList();
    }

    private List<ArtworkDetailResponse.RelatedArtworkInfo>
    getRelatedArtworks(
            UUID exhibitionId,
            Artwork artwork
    ) {

        List<Artwork> candidates =
                artworkRepository.findRelatedArtworks(
                        exhibitionId,
                        artwork.getId(),
                        artwork.getCategory(),
                        PageRequest.of(0, 10)
                );

        Set<UUID> currentArtistIds =
                artwork.getArtworkArtistMaps().stream()
                        .map(map -> map.getArtist().getId())
                        .collect(Collectors.toSet());

        List<Artwork> sameArtist = new ArrayList<>();
        List<Artwork> sameCategory = new ArrayList<>();

        for (Artwork candidate : candidates) {

            boolean isSameArtist =
                    candidate.getArtworkArtistMaps().stream()
                            .anyMatch(map ->
                                    currentArtistIds.contains(
                                            map.getArtist().getId()
                                    )
                            );

            if (isSameArtist) {
                sameArtist.add(candidate);
            } else {
                sameCategory.add(candidate);
            }
        }

        Collections.shuffle(sameCategory);

        List<Artwork> combined = new ArrayList<>();
        combined.addAll(sameArtist);
        combined.addAll(sameCategory);

        return combined.stream()
                .limit(2)
                .map(art -> {

                    boolean isSameArtist =
                            art.getArtworkArtistMaps().stream()
                                    .anyMatch(map ->
                                            currentArtistIds.contains(
                                                    map.getArtist().getId()
                                            )
                                    );

                    return convertToRelatedInfo(
                            art,
                            isSameArtist ? "artist" : "random"
                    );
                })
                .toList();
    }

    private List<ArtworkDetailResponse.RelatedArtworkInfo>
    getNavigationArtworks(
            UUID exhibitionId,
            Artwork artwork
    ) {

        List<ArtworkDetailResponse.RelatedArtworkInfo> result =
                new ArrayList<>();

        var currentZone = artwork.getExhibitionZone();

        if (currentZone == null ||
                currentZone.getOrderId() == null ||
                artwork.getOrderIndex() == null) {

            return result;
        }

        UUID zoneId = currentZone.getId();
        Integer zoneOrderId = currentZone.getOrderId();
        Integer orderIndex = artwork.getOrderIndex();

        PageRequest page = PageRequest.of(0, 1);

        List<Artwork> prev =
                artworkRepository.findPrevInSameZone(
                        exhibitionId,
                        zoneId,
                        orderIndex,
                        page
                );

        if (prev.isEmpty()) {
            prev = artworkRepository.findPrevInPrevZone(
                    exhibitionId,
                    zoneOrderId,
                    page
            );
        }

        if (!prev.isEmpty()) {
            result.add(
                    convertToRelatedInfo(prev.get(0), "prev")
            );
        }

        List<Artwork> next =
                artworkRepository.findNextInSameZone(
                        exhibitionId,
                        zoneId,
                        orderIndex,
                        page
                );

        if (next.isEmpty()) {
            next = artworkRepository.findNextInNextZone(
                    exhibitionId,
                    zoneOrderId,
                    page
            );
        }

        if (!next.isEmpty()) {
            result.add(
                    convertToRelatedInfo(next.get(0), "next")
            );
        }

        return result;
    }

    private ArtworkDetailResponse.RelatedArtworkInfo
    convertToRelatedInfo(
            Artwork artwork,
            String type
    ) {

        String artistNames =
                artwork.getArtworkArtistMaps().stream()
                        .map(aam -> {

                            if (aam.getArtistProfile() != null &&
                                    aam.getArtistProfile().getNameKo() != null) {

                                return aam.getArtistProfile().getNameKo();
                            }

                            return aam.getArtist().getNameKo();
                        })
                        .collect(Collectors.joining(", "));

        return ArtworkDetailResponse.RelatedArtworkInfo.builder()
                .id(artwork.getId())
                .title(artwork.getTitle())
                .category(artwork.getCategory())
                .artistName(artistNames)
                .mainImage(artwork.getMainImg())
                .type(type)
                .build();
    }

    private ArtworkDetailResponse.ParticipantInfo
    convertToParticipantInfo(ArtworkArtistMap map) {

        ArtistProfile profile = map.getArtistProfile();

        return ArtworkDetailResponse.ParticipantInfo.builder()
                .artistId(map.getArtist().getId())
                .profileId(profile != null ? profile.getId() : null)
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
}