package com.dolog.server.domain.bts.service;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.global.util.TextUtils;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artist.web.dto.response.ArtistSnsResponse;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.bts.entity.Bts;
import com.dolog.server.domain.bts.entity.BtsArtworkMap;
import com.dolog.server.domain.bts.repository.BtsRepository;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.bts.exception.BtsErrorCode;
import com.dolog.server.domain.bts.exception.BtsException;
import com.dolog.server.domain.bts.web.dto.request.BtsCreateRequest;
import com.dolog.server.domain.bts.web.dto.request.BtsMappingUpdateRequest;
import com.dolog.server.domain.bts.web.dto.request.BtsUpdateRequest;
import com.dolog.server.domain.bts.web.dto.response.BtsResponse;
import com.dolog.server.domain.bts.web.dto.response.BtsDetailResponse;
import com.dolog.server.domain.bts.web.dto.response.BtsListResponse;
import com.dolog.server.domain.bts.web.dto.response.BtsMappingUpdateResponse;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.artist.exception.artistError.ArtistNotFoundException;
import com.dolog.server.domain.artist.exception.artistProfileError.ArtistProfileNotFoundException;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.global.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dolog.server.global.util.FileService;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.LinkedHashSet;

@Service
@RequiredArgsConstructor
@Transactional
public class BtsServiceImpl implements BtsService {

    private final BtsRepository btsRepository;
    private final ExhibitionRepository exhibitionRepository;
    private final ArtistRepository artistRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final ArtworkRepository artworkRepository;
    private final FileService fileService;

//    1. 생성
    @Transactional
    @Override
    public BtsResponse createBts(UUID exhibitionId, BtsCreateRequest request, MultipartFile mainImg) throws IOException {
        // 1. 작품들 조회 - 요청 개수와 다르면 일부 ID가 잘못된 것
        List<Artwork> artworks = artworkRepository.findAllById(request.getArtworkIds());
        if (artworks.size() != request.getArtworkIds().size())
            throw new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND);

        Artwork representativeArtwork = artworks.get(0);
        Exhibition exhibition = representativeArtwork.getExhibition();

        // 1-1. URL의 exhibitionId와 작품의 전시 일치 검증
        if (!Objects.equals(exhibition.getId(), exhibitionId))
            throw new BtsException(BtsErrorCode.BTS_EXHIBITION_MISMATCH);

        // 1-2. 모든 작품이 동일한 전시 소속인지 검증
        boolean hasInvalidArtwork = artworks.stream()
                .anyMatch(a -> !Objects.equals(a.getExhibition().getId(), exhibitionId));
        if (hasInvalidArtwork) throw new BtsException(BtsErrorCode.ARTWORK_EXHIBITION_MISMATCH);

        // 2. 작품의 작가(Artist) 찾기
        Artist artist = representativeArtwork.getArtworkArtistMaps().stream()
                .findFirst()
                .map(ArtworkArtistMap::getArtist)
                .orElseThrow(ArtistNotFoundException::new);

        // 3. 해당 전시의 작가 프로필(ArtistProfile) 찾기 (중요!)
        ArtistProfile artistProfile = artistProfileRepository.findByArtistAndExhibition(artist, exhibition)
                .orElseThrow(ArtistProfileNotFoundException::new);

        // 4. 파일 업로드 (선택)
        String dbImageUrl = (mainImg != null && !mainImg.isEmpty())
                ? fileService.uploadFile(mainImg, "bts")
                : null;

        // 5. BTS 엔티티 생성 (ArtistProfile 저장)
        Bts bts = Bts.builder()
                .exhibition(exhibition)
                .artistProfile(artistProfile)
                .title(request.getTitle())
                .linkLabel(request.getLinkLabel())
                .linkUrl(request.getLinkUrl())
                .content(TextUtils.normalizeNewlines(request.getContent()))
                .mainImg(dbImageUrl)
                .build();

        // 5. 모든 작품을 BTS와 매핑 (N:M 처리)
        for (Artwork artwork : artworks) {
            BtsArtworkMap map = BtsArtworkMap.builder()
                    .bts(bts)
                    .artwork(artwork)
                    .build();
            bts.getArtworkMaps().add(map);
        }

        Bts savedBts = btsRepository.save(bts);
        return BtsResponse.of(savedBts);
    }

//    2. 수정
    @Override
    @Transactional
    public BtsResponse updateBts(UUID exhibitionId, UUID btsId, BtsUpdateRequest request, MultipartFile mainImg) throws IOException {
        Bts bts = btsRepository.findById(btsId)
                .orElseThrow(() -> new BtsException(BtsErrorCode.BTS_NOT_FOUND));
        if(!bts.getExhibition().getId().equals(exhibitionId)) {
            throw new BtsException(BtsErrorCode.BTS_EXHIBITION_MISMATCH);
        }

        // 2. 작가 프로필 업데이트 로직
        ArtistProfile artistProfile = bts.getArtistProfile();

        // DTO 필드명을 바꿨으므로 getArtistProfileId() 호출 가능!
        if (request.getArtistProfileId() != null) {
            artistProfile = artistProfileRepository.findById(request.getArtistProfileId())
                    .orElseThrow(ArtistProfileNotFoundException::new);
            if (!Objects.equals(artistProfile.getExhibition().getId(), exhibitionId)) {
                throw new BtsException(BtsErrorCode.ARTIST_PROFILE_EXHIBITION_MISMATCH);
            }
        }

        // 3. 업데이트 수행
        String mainImgUrl = (mainImg != null && !mainImg.isEmpty())
                ? fileService.uploadFile(mainImg, "bts")
                : null;
        bts.updateBtsInfo(request.getTitle(), request.getLinkLabel(), request.getLinkUrl(), request.getContent(), mainImgUrl, artistProfile);

        // 4. 연결된 작품 목록 업데이트 (기존 로직 동일)
        if (request.getArtworkIds() != null) {
            bts.getArtworkMaps().clear();
            List<Artwork> artworks = artworkRepository.findAllById(request.getArtworkIds());
            for (Artwork artwork : artworks) {
                BtsArtworkMap map = BtsArtworkMap.builder()
                        .bts(bts)
                        .artwork(artwork)
                        .build();
                bts.getArtworkMaps().add(map);
            }
        }

        return BtsResponse.of(bts);
    }

    @Override
    @Transactional
    public void deleteBts(UUID exhibitionId, UUID btsId) {
        Bts bts = btsRepository.findById(btsId)
                .orElseThrow(() -> new BtsException(BtsErrorCode.BTS_NOT_FOUND));
        if(!bts.getExhibition().getId().equals(exhibitionId)) {
            throw new BtsException(BtsErrorCode.BTS_EXHIBITION_MISMATCH);
        }

        // cascade = ALL 설정에 의해 bts_artwork_map도 같이 삭제됨
        btsRepository.delete(bts);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BtsListResponse> getExhibitionBtsList(UUID exhibitionId) {
        // 1. 해당 전시회의 모든 BTS 조회
        List<Bts> btsList = btsRepository.findAllByExhibitionId(exhibitionId);

        // 2. DTO 리스트로 변환
        return btsList.stream()
                .map(BtsListResponse::from)
                .collect(Collectors.toList());
    }


//    3. BTS 상세 조회
    /**
     * BTS 상세 조회
     * 로직: 본체 조회 -> 작가 정보 매핑 -> 연관 작품 매핑 -> 추천 목록(작가 우선) 생성
     */
    @Override
    @Transactional(readOnly = true)
    public BtsDetailResponse getBtsDetail(UUID exhibitionId, UUID btsId) {
        // 1. BTS 본체 조회 (정의하신 BtsException 사용)
        Bts bts = btsRepository.findById(btsId)
                .orElseThrow(() -> new BtsException(BtsErrorCode.BTS_NOT_FOUND));
        if(!bts.getExhibition().getId().equals(exhibitionId)) {
            throw new BtsException(BtsErrorCode.BTS_EXHIBITION_MISMATCH);
        }

        // 2. 작가 프로필 정보 매핑 (Bts -> ArtistProfile)
        ArtistProfile profile = bts.getArtistProfile();
        List<BtsDetailResponse.BtsArtistProfileInfo> artists = new ArrayList<>();

        if (profile != null) {
            artists.add(BtsDetailResponse.BtsArtistProfileInfo.builder()
                    .participantId(profile.getId())
                    .nameKo(profile.getNameKo())
                    .nameEn(profile.getNameEn())
                    .profileImage(profile.getProfileImg())
                    .bio(profile.getBio())
                    .contact(BtsDetailResponse.ContactInfo.builder()
                            .email(profile.getEmail())
                            .sns(profile.getSnsList().stream()
                                    .map(ArtistSnsResponse::from)
                                    .toList())
                            .build())
                    .build());
        }

        // 3. 연관 작품 매핑 (BtsArtworkMap을 거쳐서 Artwork 추출)
        List<BtsDetailResponse.RelatedArtworkInfo> relatedArtworks = bts.getArtworkMaps().stream()
                .map(BtsArtworkMap::getArtwork)
                .map(artwork -> BtsDetailResponse.RelatedArtworkInfo.builder()
                        .artworkId(artwork.getId())
                        .title(artwork.getTitle())
                        .image(artwork.getMainImg())
                        .build())
                .toList();

        // 4. 추천 BTS 리스트 생성 (우선순위: 동일 작가 -> 동일 전시 최신순)
        List<BtsDetailResponse.RecommendedBtsInfo> recommendedBts = getRecommendedBtsList(bts);

        // 5. 최종 Response 반환
        return BtsDetailResponse.builder()
                .btsId(bts.getId())
                .title(bts.getTitle())
                .linkLabel(bts.getLinkLabel())
                .linkUrl(bts.getLinkUrl())
                .content(TextUtils.normalizeNewlines(bts.getContent()))
                .mainImg(bts.getMainImg())
                .artists(artists)
                .relatedArtworks(relatedArtworks)
                .recommendedBts(recommendedBts)
                .build();
    }

    /**
     * 추천 로직 분리 (Private Method)
     */
    private List<BtsDetailResponse.RecommendedBtsInfo> getRecommendedBtsList(Bts bts) {
        Set<Bts> recommendedSet = new LinkedHashSet<>();
        UUID exId = bts.getExhibition().getId();
        UUID currentBtsId = bts.getId();

        // 1. 작가 프로필 존재 여부 체크 (NPE 방지 핵심)
        if (bts.getArtistProfile() != null) {
            UUID artistProfileId = bts.getArtistProfile().getId();

            // [우선순위 1] 같은 전시 내 동일 작가의 다른 글 추가
            List<Bts> sameArtistBts = btsRepository.findTop3ByExhibitionIdAndArtistProfileIdAndIdNotOrderByCreatedAtDesc(
                    exId, artistProfileId, currentBtsId);
            recommendedSet.addAll(sameArtistBts);
        }

        // 2. 3개가 채워지지 않았다면 (또는 작가가 없다면) 전시회 내 다른 글 추가
        if (recommendedSet.size() < 3) {
            List<Bts> exhibitionBts = btsRepository.findTop3ByExhibitionIdAndIdNotOrderByCreatedAtDesc(
                    exId, currentBtsId);

            for (Bts rb : exhibitionBts) {
                recommendedSet.add(rb); // LinkedHashSet이라 순서 유지 + 중복 자동 제거
                if (recommendedSet.size() >= 3) break;
            }
        }

        return recommendedSet.stream()
                .map(rb -> BtsDetailResponse.RecommendedBtsInfo.builder()
                        .btsId(rb.getId())
                        .title(rb.getTitle())
                        .mainImg(rb.getMainImg())
                        .build())
                .toList();
    }

//    =========================

    @Override
    @Transactional
    public BtsMappingUpdateResponse syncBtsMapping(UUID exhibitionId, UUID btsId, BtsMappingUpdateRequest request) {
        // 1. Exhibition 존재 여부 확인
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        // 2. BTS 조회 및 해당 전시 소속 검증
        Bts bts = btsRepository.findById(btsId)
                .orElseThrow(() -> new BtsException(BtsErrorCode.BTS_NOT_FOUND));
        if (!bts.getExhibition().getId().equals(exhibitionId)) {
            throw new BtsException(BtsErrorCode.BTS_EXHIBITION_MISMATCH);
        }

        // 3. ArtistProfile 조회 및 해당 전시 소속 검증
        ArtistProfile artistProfile = artistProfileRepository.findById(request.getArtistProfileId())
                .orElseThrow(ArtistProfileNotFoundException::new);
        if (!Objects.equals(artistProfile.getExhibition().getId(), exhibitionId)) {
            throw new BtsException(BtsErrorCode.ARTIST_PROFILE_EXHIBITION_MISMATCH);
        }

        // 4. BTS 기본 정보 업데이트 (title, content, artist)
        bts.updateBtsInfo(request.getTitle(), request.getLinkLabel(), request.getLinkUrl(), null, null, artistProfile);

        // 5. 작품 매핑 교체 - 조회 결과가 요청 개수와 다르면 일부 ID가 잘못된 것
        List<Artwork> artworks = artworkRepository.findAllById(request.getArtworkIds());
        if (artworks.size() != request.getArtworkIds().size()) {
            throw new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND);
        }
        boolean hasInvalidArtwork = artworks.stream()
                .anyMatch(a -> !Objects.equals(a.getExhibition().getId(), exhibitionId));
        if (hasInvalidArtwork) {
            throw new BtsException(BtsErrorCode.ARTWORK_EXHIBITION_MISMATCH);
        }
        bts.getArtworkMaps().clear();
        for (Artwork artwork : artworks) {
            BtsArtworkMap map = BtsArtworkMap.builder()
                    .bts(bts)
                    .artwork(artwork)
                    .build();
            bts.getArtworkMaps().add(map);
        }

        // 6. 응답 생성
        List<UUID> updatedArtworkIds = bts.getArtworkMaps().stream()
                .map(map -> map.getArtwork().getId())
                .collect(Collectors.toList());

        return BtsMappingUpdateResponse.builder()
                .exhibitionId(exhibitionId)
                .btsId(bts.getId())
                .title(bts.getTitle())
                .artistProfileId(artistProfile.getId())
                .updatedArtworkIds(updatedArtworkIds)
                .build();
    }



}
