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
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 조회 전용이므로 readOnly 설정
public class ArtworkDetailServiceImpl implements ArtworkDetailService {

    private final ArtworkRepository artworkRepository;
    private final BtsRepository btsRepository;
    private final ExhibitionGuideMapRepository guideMapRepository;

    @Override
    public ArtworkDetailResponse getArtworkDetail(UUID exhibitionId, UUID artworkId) {
        // 1. 작품 상세 정보 조회 (이미지, 작가, SNS까지 fetch join으로 가져온다고 가정)
        Artwork artwork = artworkRepository.findDetailById(exhibitionId, artworkId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        // 2. 해당 작품과 연관된 BTS 콘텐츠 조회
        List<ArtworkDetailResponse.RelatedBtsInfo> relatedBts = btsRepository.findAllByArtworkId(artworkId).stream()
                .map(bts -> ArtworkDetailResponse.RelatedBtsInfo.builder()
                        .id(bts.getId())
                        .title(bts.getTitle())
                        .mainImg(bts.getMainImg())
                        .build())
                .toList();

        // 4. 최종 DTO 조립
        return ArtworkDetailResponse.builder()
                .title(artwork.getTitle())
                .category(artwork.getCategory())
                .material(artwork.getMaterial())
                .size(artwork.getSize())
                .description(artwork.getDescription())
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
                .build();
    }

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
                .sns(p != null ? p.getSnsList().stream()
                        .map(sns -> ArtworkDetailResponse.SnsInfo.builder()
                                .platformName(sns.getPlatformName())
                                .url(sns.getUrl())
                                .build()).toList() : Collections.emptyList())
                .build();
    }
}
