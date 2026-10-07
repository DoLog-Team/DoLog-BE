package com.dolog.server.domain.artwork.service.artwork.query;

import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.entity.ArtworkMaterial;
import com.dolog.server.domain.artwork.entity.ExhibitionFieldSettings;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.repository.ExhibitionFieldSettingsRepository;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkDetailResponse;
import com.dolog.server.domain.bts.repository.BtsRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionArtistMapRepository;
import com.dolog.server.domain.like.repository.ArtworkLikeRepository;
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
    private final ArtworkLikeRepository artworkLikeRepository;
    private final ExhibitionFieldSettingsRepository fieldSettingsRepository;
    private final ExhibitionArtistMapRepository exhibitionArtistMapRepository;

    // 전시 URL: 공개 + 작품 숨김 아님 + 구역 숨김 아님
    @Transactional
    public ArtworkDetailResponse getArtworkDetail(
            UUID exhibitionId,
            UUID artworkId,
            String visitorId
    ) {

        Artwork artwork = artworkRepository.findDetailById(exhibitionId, artworkId)
                .filter(Artwork::isVisibleOnExhibition)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        return toDetail(artwork, true, visitorId);
    }

    // 두록 URL: 공개면 숨김과 상관없이 노출
    @Transactional
    public ArtworkDetailResponse getDologArtworkDetail(
            UUID artworkId,
            String visitorId
    ) {

        Artwork artwork = artworkRepository.findById(artworkId)
                .filter(Artwork::isVisibleOnDolog)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        return toDetail(artwork, false, visitorId);
    }

    private ArtworkDetailResponse toDetail(
            Artwork artwork,
            boolean exhibitionView,
            String visitorId
    ) {

        UUID artworkId = artwork.getId();
        UUID exhibitionId = artwork.getExhibition() != null ? artwork.getExhibition().getId() : null;

        artworkRepository.increaseViewCount(artworkId);

        ExhibitionFieldSettings settings = exhibitionId != null
                ? fieldSettingsRepository.findByExhibitionId(exhibitionId).orElse(null)
                : null;
        boolean hideSize = settings != null && settings.isHiddenSize();
        boolean hideMaterials = settings != null && settings.isHiddenMaterials();
        boolean hideLocationMap = settings != null && settings.isHiddenLocationMap();
        // 제작 연도를 숨기면 제작 기간 전체를, 제작 기간을 숨기면 연도만 남긴다.
        boolean hideYear = settings != null && settings.isHiddenProductionYear();
        boolean hideMonthDay = hideYear || (settings != null && settings.isHiddenProductionPeriod());

        Set<UUID> joinedArtistIds = getJoinedArtistIds(exhibitionId, artwork);

        List<ArtworkDetailResponse.RelatedArtworkInfo> relatedArtworks = exhibitionId != null
                ? getRelatedArtworks(exhibitionId, artwork, exhibitionView)
                : List.of();
        List<ArtworkDetailResponse.RelatedArtworkInfo> navigationArtworks = exhibitionId != null
                ? getNavigationArtworks(exhibitionId, artwork, exhibitionView)
                : List.of();

        return ArtworkDetailResponse.builder()
                .title(artwork.getTitle())
                .category(artwork.getCategory())
                .description(artwork.getDescription())
                .shortIntro(artwork.getShortIntro())
                .materials(hideMaterials
                        ? List.of()
                        : artwork.getMaterials().stream().map(ArtworkMaterial::getName).toList())
                .width(hideSize ? null : artwork.getWidth())
                .height(hideSize ? null : artwork.getHeight())
                .depth(hideSize ? null : artwork.getDepth())
                .productionStartYear(hideYear ? null : artwork.getProductionStartYear())
                .productionStartMonth(hideMonthDay ? null : artwork.getProductionStartMonth())
                .productionStartDay(hideMonthDay ? null : artwork.getProductionStartDay())
                .productionEndYear(hideYear ? null : artwork.getProductionEndYear())
                .productionEndMonth(hideMonthDay ? null : artwork.getProductionEndMonth())
                .productionEndDay(hideMonthDay ? null : artwork.getProductionEndDay())
                .purchaseUrl(artwork.getPurchaseUrl())
                .purchaseChatUrl(artwork.getPurchaseChatUrl())
                .showPurchaseButton(artwork.getShowPurchaseButton())
                .youtubeUrl(artwork.getYoutubeUrl())
                .mainImage(artwork.getMainImg())
                .locationMap(hideLocationMap ? null : artwork.getLocationMap())
                .viewCount(artwork.getViewCount() + 1)
                .likeCount(artworkLikeRepository.countByArtworkId(artworkId))
                .liked(visitorId != null && artworkLikeRepository.existsByArtworkIdAndVisitorId(artworkId, visitorId))
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
                                .map(map -> convertToParticipantInfo(
                                        map,
                                        exhibitionId == null || joinedArtistIds.contains(map.getArtist().getId())
                                ))
                                .toList()
                )
                .relatedBts(getRelatedBts(artworkId))
                .sameCategoryArtworks(relatedArtworks)
                .alphabeticalArtworks(navigationArtworks)
                .build();
    }

    // 전시에서 제외되거나 나간 작가는 이름과 역할만 남기고 프로필은 내보내지 않는다 (#352).
    private Set<UUID> getJoinedArtistIds(UUID exhibitionId, Artwork artwork) {

        if (exhibitionId == null) {
            return Set.of();
        }

        List<UUID> artistIds = artwork.getArtworkArtistMaps().stream()
                .map(map -> map.getArtist().getId())
                .toList();

        if (artistIds.isEmpty()) {
            return Set.of();
        }

        return new HashSet<>(exhibitionArtistMapRepository.findJoinedArtistIds(exhibitionId, artistIds));
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
            Artwork artwork,
            boolean exhibitionView
    ) {

        List<Artwork> candidates =
                artworkRepository.findRelatedArtworks(
                        exhibitionId,
                        artwork.getId(),
                        artwork.getCategory(),
                        exhibitionView,
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
            Artwork artwork,
            boolean exhibitionView
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
                        exhibitionView,
                        page
                );

        if (prev.isEmpty()) {
            prev = artworkRepository.findPrevInPrevZone(
                    exhibitionId,
                    zoneOrderId,
                    exhibitionView,
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
                        exhibitionView,
                        page
                );

        if (next.isEmpty()) {
            next = artworkRepository.findNextInNextZone(
                    exhibitionId,
                    zoneOrderId,
                    exhibitionView,
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
    convertToParticipantInfo(ArtworkArtistMap map, boolean profileAccessible) {

        ArtistProfile profile = map.getArtistProfile();
        boolean showProfile = profile != null && profileAccessible;

        return ArtworkDetailResponse.ParticipantInfo.builder()
                .artistId(map.getArtist().getId())
                .profileId(showProfile ? profile.getId() : null)
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
                .profileImg(showProfile ? profile.getProfileImg() : null)
                .role(map.getArtistRole())
                .bio(showProfile ? profile.getBio() : null)
                .email(showProfile ? profile.getEmail() : null)
                .sns(
                        showProfile
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
