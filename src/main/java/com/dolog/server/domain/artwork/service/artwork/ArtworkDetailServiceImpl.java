package com.dolog.server.domain.artwork.service.artwork;

import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkDetailResponse;
import com.dolog.server.domain.bts.repository.BtsRepository;
import com.dolog.server.domain.exhibition.entity.ExhibitionGuideMap;
import com.dolog.server.domain.exhibition.repository.ExhibitionGuideMapRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtworkDetailServiceImpl implements ArtworkDetailService {

    private final ArtworkRepository artworkRepository;
    private final BtsRepository btsRepository;
    private final ExhibitionGuideMapRepository guideMapRepository;

    @Override
    public ArtworkDetailResponse getArtworkDetail(UUID exhibitionId, UUID artworkId) {
        // 1. 작품 상세 정보 조회
        Artwork artwork = artworkRepository.findDetailById(exhibitionId, artworkId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        // 2. 해당 작품과 연관된 BTS 콘텐츠 조회
        List<ArtworkDetailResponse.RelatedBtsInfo> relatedBts = btsRepository.findAllByArtworkId(artworkId).stream()
                .map(bts -> {
                    String authorName = (bts.getArtistProfile() != null)
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

        // ==============================================================================
        // 3. 동일 작가 작품 우선 (카테고리 무관) / 이후 동일 카테고리 내 랜덤 (2개)
        // ==============================================================================
        List<ArtworkDetailResponse.RelatedArtworkInfo> sameCatList = new ArrayList<>();
        int maxCategoryItems = 2;

        // 3-1. '동일 작가'의 다른 작품
        List<Artwork> sameArtistArtworks = artworkRepository.findBySameArtists(exhibitionId, artworkId);
        for (Artwork art : sameArtistArtworks) {
            if (sameCatList.size() >= maxCategoryItems) {
                break;
            }
            sameCatList.add(convertToRelatedInfo(art, "artist"));
        }

        // 3-2. '동일 카테고리' 내 '다른 작가'의 작품 랜덤
        if (sameCatList.size() < maxCategoryItems) {
            int neededCount = maxCategoryItems - sameCatList.size();

            List<Artwork> randomArtworks = artworkRepository.findSameCategoryRandom(
                    exhibitionId, artwork.getCategory(), artworkId, PageRequest.of(0, neededCount)
            );
            for (Artwork art : randomArtworks) {
                sameCatList.add(convertToRelatedInfo(art, "random"));
            }
        }

        // ==============================================================================
        // 4. 존(Zone) 별 orderIndex 기준 조회
        // ==============================================================================
        List<ArtworkDetailResponse.RelatedArtworkInfo> alphaList = new ArrayList<>();

        var currentZone = artwork.getExhibitionZone();
        UUID zoneId = currentZone != null ? currentZone.getId() : null;
        Integer zoneOrderId = currentZone != null ? currentZone.getOrderId() : null; // 엔티티의 orderId 필드 추출
        Integer orderIndex = artwork.getOrderIndex();

        if (zoneId != null && zoneOrderId != null && orderIndex != null) {

            // 4-1. 이전
            List<Artwork> prevArtworks = artworkRepository.findGlobalPrevArtwork(
                    exhibitionId, zoneId, zoneOrderId, orderIndex, PageRequest.of(0, 1)
            );
            if (!prevArtworks.isEmpty()) {
                alphaList.add(convertToRelatedInfo(prevArtworks.get(0), "prev"));
            }

            // 4-2. 다음
            List<Artwork> nextArtworks = artworkRepository.findGlobalNextArtwork(
                    exhibitionId, zoneId, zoneOrderId, orderIndex, PageRequest.of(0, 1)
            );
            if (!nextArtworks.isEmpty()) {
                alphaList.add(convertToRelatedInfo(nextArtworks.get(0), "next"));
            }
        }

        // 5. 최종 DTO 조립 및 반환
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
                .detailImages(artwork.getArtworkImg().stream()
                        .map(img -> ArtworkDetailResponse.DetailImageInfo.builder()
                                .imageUrl(img.getImageUrl())
                                .description(img.getDescription())
                                .build())
                        .toList())
                .participants(artwork.getArtworkArtistMaps().stream()
                        .map(this::convertToParticipantInfo)
                        .toList())
                .relatedBts(relatedBts)
                .sameCategoryArtworks(sameCatList)
                .alphabeticalArtworks(alphaList)
                .build();
    }

    // ==============================================================================
    // 매핑 헬퍼
    // ==============================================================================

    // 관련 작품 정보 매핑
    private ArtworkDetailResponse.RelatedArtworkInfo convertToRelatedInfo(Artwork artwork, String type) {
        String combinedArtistNames = artwork.getArtworkArtistMaps().stream()
                .map(aam -> {
                    if (aam.getArtistProfile() != null && aam.getArtistProfile().getNameKo() != null) {
                        return aam.getArtistProfile().getNameKo();
                    }
                    return aam.getArtist().getNameKo();
                })
                .collect(Collectors.joining(", "));

        return ArtworkDetailResponse.RelatedArtworkInfo.builder()
                .id(artwork.getId())
                .title(artwork.getTitle())
                .category(artwork.getCategory())
                .artistName(combinedArtistNames)
                .mainImage(artwork.getMainImg())
                .type(type)
                .build();
    }

    // 참가 작가 상세 정보 매핑
    private ArtworkDetailResponse.ParticipantInfo convertToParticipantInfo(ArtworkArtistMap map) {
        ArtistProfile p = map.getArtistProfile();
        return ArtworkDetailResponse.ParticipantInfo.builder()
                .artistId(map.getArtist().getId())
                .profileId(p != null ? p.getId() : null)
                .nameKo(p != null ? p.getNameKo() : map.getArtist().getNameKo())
                .nameEn(p != null ? p.getNameEn() : map.getArtist().getNameEn())
                .profileImg(p != null ? p.getProfileImg() : null)
                .role(map.getArtistRole())
                .bio(p != null ? p.getBio() : null)
                .email(p != null ? p.getEmail() : null)
                .sns(p != null ? p.getSnsList().stream()
                        .map(sns -> ArtworkDetailResponse.SnsInfo.builder()
                                .platformName(sns.getPlatformName())
                                .url(sns.getUrl())
                                .build()).toList() : Collections.emptyList())
                .build();
    }
}