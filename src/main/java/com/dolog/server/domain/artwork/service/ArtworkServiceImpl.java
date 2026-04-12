package com.dolog.server.domain.artwork.service;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.exception.artistError.ArtistNotFoundException;
import com.dolog.server.domain.artist.exception.artistProfileError.ArtistProfileNotFoundException;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.entity.ArtworkImg;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ArtworkArtistMapRepository;
import com.dolog.server.domain.artwork.repository.ArtworkImgRepository;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.repository.ArtworkSpecification;
import com.dolog.server.domain.artwork.web.dto.request.*;
import com.dolog.server.domain.artwork.web.dto.response.*;
import com.dolog.server.domain.exhibition.entity.*;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionDetailRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionGuideMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionZoneRepository;
import com.dolog.server.domain.exhibition.web.dto.response.artwork.ExhibitionArtworkListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtworkServiceImpl implements ArtworkService {

    private final ArtworkRepository artworkRepository;
    private final ArtworkArtistMapRepository artworkArtistMapRepository;
    private final ExhibitionDetailRepository exhibitionDetailRepository;
    private final ExhibitionRepository exhibitionRepository;
    private final ExhibitionZoneRepository exhibitionZoneRepository;
    private final ArtworkImgRepository artworkImgRepository;
    private final ExhibitionGuideMapRepository exhibitionGuideMapRepository;
    private final ArtistProfileRepository artistProfileRepository;

    /**
     * 작품 전체 목록 조회
     * - category, search 파라미터로 필터링 가능
     * - main=true 이면 카테고리별 그룹화 응답, false/null 이면 flat 배열 응답
     */
    @Override
    @Transactional(readOnly = true)
    public Object getArtworks(Boolean main, String category, String search) {
        // 1. 조건에 맞는 작품 목록 조회 (category/search 필터 적용)
        Specification<Artwork> spec = Specification
                .where(ArtworkSpecification.withExhibitionFetch())
                .and(ArtworkSpecification.withCategory(category))
                .and(ArtworkSpecification.withSearch(search));
        List<Artwork> artworks = artworkRepository.findAll(spec);

        // 2. 결과 없으면 빈 응답 반환
        if (artworks.isEmpty()) {
            if (Boolean.TRUE.equals(main)) {
                return MainCategoryResponse.builder().categories(Collections.emptyList()).build();
            } else {
                return Collections.emptyList();
            }
        }

        // 3. 작품 ID / 전시회 ID 목록 추출 (후속 쿼리용)
        List<UUID> artworkIds = artworks.stream().map(Artwork::getId).collect(Collectors.toList());
        List<UUID> exhibitionIds = artworks.stream().map(a -> a.getExhibition().getId()).distinct().collect(Collectors.toList());

        // 4. 작품별 작가명, 전시회별 전시 제목 일괄 조회 (N+1 방지)
        Map<UUID, String> artistMap = fetchArtistMap(artworkIds);
        Map<UUID, String> exhibitionDetailMap = fetchExhibitionDetailMap(exhibitionIds);

        // 5. main 여부에 따라 응답 형태 분기
        if (Boolean.TRUE.equals(main)) {
            return buildMainCategoryResponse(artworks, artistMap, exhibitionDetailMap);
        } else {
            return buildArtworkListResponse(artworks, artistMap, exhibitionDetailMap);
        }
    }

    /**
     * 작품 기본 정보 등록
     */
    @Override
    public ArtworkCreateResponse createArtwork(ArtworkCreateRequest request) {
        // 1. ArtistProfile 조회 (여기서 Artist와 Exhibition 정보를 한 번에 가져옴)
        // fetch join을 사용하면 성능상 더 이득입니다.
        ArtistProfile profile = artistProfileRepository.findById(request.getArtistProfileId())
                .orElseThrow(() -> new RuntimeException("Artist Profile not found"));

        // ArtworkServiceImpl.java
        Artwork artwork = Artwork.builder()
                .exhibition(profile.getExhibition())
                //.exhibitionZone(exhibitionZone)
                .title(request.getTitle())
                .category(request.getCategory())
                .material(request.getMaterial())
                .size(request.getSize())
                .description(request.getDescription())
                .mainImg(request.getMainImage())
                .purchaseUrl(request.getPurchaseUrl())
                .orderIndex(request.getOrderIndex())
                .build();

        // 3. 매핑 객체 생성 (여기서 profile을 꼭 넣어주세요!)
        ArtworkArtistMap artistMap = ArtworkArtistMap.builder()
                .artwork(artwork)
                .artist(profile.getArtist())      // Artist 연결
                .artistProfile(profile)           // ArtistProfile 연결
                .artistRole(request.getArtistRole() != null ? request.getArtistRole() : "Artist")
                .build();

        // 4. 양방향 연관관계 설정 (Cascade에 의해 함께 저장됨)
        artwork.getArtworkArtistMaps().add(artistMap);

        // 5. 저장
        Artwork saved = artworkRepository.save(artwork);

        // 6. 확장된 Response 반환
        // profile.getNameKo()를 통해 등록된 작가 이름도 함께 전달합니다.
        return ArtworkCreateResponse.of(saved, profile.getNameKo());
    }

    /**
     * 작품 상세 이미지 등록
     */
    @Override
    public ArtworkImgCreateResponse createArtworkImages(UUID artworkId, List<ArtworkImgCreateRequest> requests) {
        // 작품 존재 여부 확인
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new RuntimeException("Artwork not found")); // 적절한 예외 처리 필요

        List<ArtworkImg> imgs = requests.stream()
                .map(req -> ArtworkImg.builder()
                        .artwork(artwork)
                        .imageUrl(req.getImageUrl())
                        .description(req.getDescription())
                        .orderIndex(req.getOrderIndex())
                        .build())
                .collect(Collectors.toList());

        List<ArtworkImg> savedImgs = artworkImgRepository.saveAll(imgs);
        List<UUID> imgIds = savedImgs.stream().map(ArtworkImg::getId).collect(Collectors.toList());

        return ArtworkImgCreateResponse.from(artworkId, imgIds);
    }

    /**
     * 작품 상세 이미지 수정
     */
    @Override
    @Transactional
    public ArtworkImgUpdateResponse updateArtworkImage(UUID artworkId, UUID imageId, ArtworkImgUpdateRequest request) {
        // 1. 작품(부모) 존재 여부부터 확인
        // 요청된 artworkId 자체가 잘못되었다면 여기서 에러 발생
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        // 2. 이미지(자식) 존재 확인
        ArtworkImg artworkImg = artworkImgRepository.findById(imageId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_IMAGE_NOT_FOUND));

        // 3. 소속 검증
        // 위에서 조회한 artwork 객체와 artworkImg가 가진 artwork 객체가 같은지 비교합니다.
        if (!artworkImg.getArtwork().getId().equals(artwork.getId())) {
            throw new ArtworkException(ArtworkErrorCode.INVALID_ARTWORK_IMAGE);
        }

        // 4. 업데이트 수행
        artworkImg.update(
                request.getImageUrl(),
                request.getDescription(),
                request.getOrderIndex()
        );

        return new ArtworkImgUpdateResponse(artworkImg.getId());
    }

    @Override
    @Transactional
    public void deleteArtworkImage(UUID artworkId, UUID imageId) {
        // 1. 이미지 조회
        ArtworkImg artworkImg = artworkImgRepository.findById(imageId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_IMAGE_NOT_FOUND));

        // 2. 검증
        if (!artworkImg.getArtwork().getId().equals(artworkId)) {
            throw new ArtworkException(ArtworkErrorCode.INVALID_ARTWORK_IMAGE);
        }

        // 3. 삭제
        artworkImgRepository.delete(artworkImg);
    }

    /**
     * 작품 기본 정보 수정 (추가)
     */
    @Override
    @Transactional
    public ArtworkCreateResponse updateArtwork(UUID artworkId, ArtworkUpdateRequest request) {
        // 1. 작품 조회 (작품을 가져오면 그 안에 이미 Exhibition 정보가 들어있음)
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        // 2. 작가 정보 처리 (새 프로필 ID가 오면 업데이트, 아니면 기존 유지)
        String currentArtistName;
        if (request.getArtistProfileId() != null) {
            ArtistProfile newProfile = artistProfileRepository.findById(request.getArtistProfileId())
                    .orElseThrow(() -> new RuntimeException("Artist Profile not found"));

            if (!artwork.getArtworkArtistMaps().isEmpty()) {
                ArtworkArtistMap map = artwork.getArtworkArtistMaps().get(0);
                map.updateArtistProfile(newProfile.getArtist(), newProfile, request.getArtistRole());
            }
            currentArtistName = newProfile.getNameKo();
        } else {
            currentArtistName = artwork.getArtworkArtistMaps().stream()
                    .findFirst()
                    .map(m -> m.getArtistProfile() != null ? m.getArtistProfile().getNameKo() : m.getArtist().getNameKo())
                    .orElse("Unknown Artist");
        }

        // 3. 전시 구역(Zone) 처리
        ExhibitionZone zone = null;
        if (request.getZoneId() != null) {
            zone = exhibitionZoneRepository.findById(request.getZoneId()).orElse(null);
        }

        // 4. 정보 업데이트 (순서: title, description, category, zone, material, size, mainImg, purchaseUrl)
        artwork.updateAllInfo(
                request.getTitle(),
                request.getDescription(),
                request.getCategory(),
                zone,
                request.getMaterial(),
                request.getSize(),
                request.getMainImage(),
                request.getPurchaseUrl()
        );

        // 5. 결과 반환
        return ArtworkCreateResponse.of(artwork, currentArtistName);
    }

    /**
     * 작품 삭제 (추가)
     */
    @Override
    @Transactional
    public void deleteArtwork(UUID artworkId) {
        // 1. 삭제할 작품이 존재하는지 조회
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        // 2. 삭제 실행
        // (Artwork 엔티티에 설정된 cascade에 의해 ArtworkImg, ArtworkArtistMap 등도 함께 삭제됨)
        artworkRepository.delete(artwork);
    }

    // 등록 (POST)
    @Override
    @Transactional
    public ArtworkArtistMappingResponse createArtistMapping(UUID artworkId, ArtworkArtistMappingRequest request) {
        // 1. 작품 조회
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        // 2. 아티스트 프로필 조회
        ArtistProfile profile = artistProfileRepository.findById(request.getArtistProfileId())
                .orElseThrow(ArtistProfileNotFoundException::new);

        // 3. 검증: 작품의 전시 ID와 아티스트 프로필의 전시 ID가 일치하는지 확인 (중요!)
        if (!artwork.getExhibition().getId().equals(profile.getExhibition().getId())) {
            throw new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND); // 같은 전시가 아님
        }

        // 4. 중복 체크 (프로필 ID 기준)
        if (artworkArtistMapRepository.existsByArtworkIdAndArtistProfileId(artworkId, profile.getId())) {
            throw new RuntimeException("이미 이 전시에 등록된 작가 프로필입니다.");
        }

        // 5. 매핑 생성 (Artist와 ArtistProfile 모두 저장)
        ArtworkArtistMap map = ArtworkArtistMap.builder()
                .artwork(artwork)
                .artist(profile.getArtist()) // 프로필 내부의 Artist 참조
                .artistProfile(profile)      // 새로 추가된 프로필 필드
                .artistRole(request.getArtistRole())
                .build();

        return ArtworkArtistMappingResponse.of(artworkArtistMapRepository.save(map));
    }

    // 수정 (PATCH)
    @Override
    @Transactional
    public ArtworkArtistMappingResponse updateArtistMapping(UUID artworkId, UUID artistProfileId, ArtworkArtistMappingRequest request) {
        // profileId를 기반으로 매핑 데이터 조회
        ArtworkArtistMap map = artworkArtistMapRepository.findByArtworkIdAndArtistProfileId(artworkId, artistProfileId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        // 역할 수정
        map.updateRole(request.getArtistRole());

        return ArtworkArtistMappingResponse.of(map);
    }

    // 삭제 (DELETE)
    @Override
    @Transactional
    public void deleteArtistMapping(UUID artworkId, UUID artistProfileId) {
        ArtworkArtistMap map = artworkArtistMapRepository.findByArtworkIdAndArtistProfileId(artworkId, artistProfileId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        artworkArtistMapRepository.delete(map);
    }

    /**
     * 작품 ID 목록으로 작품별 작가명을 Map으로 반환
     * - 작가가 여러 명이면 ", "로 합쳐서 반환 (ex. "홍길동, 김철수")
     * - key: artworkId, value: 작가명
     */
    private Map<UUID, String> fetchArtistMap(List<UUID> artworkIds) {
        // ArtworkArtistMap 조회 시 ArtistProfile도 같이 fetch join 하도록 Repository를 구성하는 것이 좋습니다.
        List<ArtworkArtistMap> artistMaps = artworkArtistMapRepository.findByArtworkIdIn(artworkIds);

        return artistMaps.stream()
                .collect(Collectors.groupingBy(
                        aam -> aam.getArtwork().getId(),
                        Collectors.mapping(aam -> {
                            // 1순위: ArtistProfile의 이름, 2순위: Artist 엔티티의 기본 이름
                            if (aam.getArtistProfile() != null && aam.getArtistProfile().getNameKo() != null) {
                                return aam.getArtistProfile().getNameKo();
                            }
                            return aam.getArtist().getNameKo();
                        }, Collectors.joining(", "))
                ));
    }

    /**
     * 전시회 ID 목록으로 전시회별 전시 제목을 Map으로 반환
     * - key: exhibitionId, value: 전시 제목
     */
    private Map<UUID, String> fetchExhibitionDetailMap(List<UUID> exhibitionIds) {
        List<ExhibitionDetail> details = exhibitionDetailRepository.findByExhibitionIdIn(exhibitionIds);
        return details.stream()
                .collect(Collectors.toMap(
                        ed -> ed.getExhibition().getId(), // Key: Exhibition의 UUID
                        ExhibitionDetail::getTitle,
                        (existing, replacement) -> existing  // 동일 전시회 중복 시 첫 번째 값 유지
                ));
    }

    /**
     * 작품 목록을 flat 배열 형태의 응답으로 변환 (main=false)
     */
    private List<ArtworkListResponse> buildArtworkListResponse(List<Artwork> artworks, Map<UUID, String> artistMap, Map<UUID, String> exhibitionDetailMap) {
        return artworks.stream()
                .map(a -> {
                    // 1. Exhibition ID로 맵에서 제목 찾기
                    UUID exId = a.getExhibition().getId();
                    String exTitle = exhibitionDetailMap.getOrDefault(exId, "Unknown Exhibition");

                    // 2. Artwork ID로 맵에서 작가명 찾기
                    String artistName = artistMap.getOrDefault(a.getId(), "Unknown Artist");

                    return ArtworkListResponse.builder()
                            .id(a.getId())
                            .title(a.getTitle())
                            .category(a.getCategory())
                            .imageUrl(a.getMainImg())
                            .exhibitionTitle(exTitle)
                            .artistName(artistMap.getOrDefault(a.getId(), "Unknown Artist"))
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 작품 목록을 카테고리별로 그룹화한 응답으로 변환 (main=true)
     * - 카테고리당 orderIndex 오름차순으로 최대 3개만 포함
     */
    private MainCategoryResponse buildMainCategoryResponse(List<Artwork> artworks, Map<UUID, String> artistMap, Map<UUID, String> exhibitionDetailMap) {
        // 카테고리별로 작품 그룹화
        Map<String, List<Artwork>> groupedByCategory = artworks.stream()
                .collect(Collectors.groupingBy(a -> a.getCategory() != null ? a.getCategory() : "Uncategorized"));

        List<CategoryArtworkResponse> categoryResponses = groupedByCategory.entrySet().stream()
                .map(entry -> {
                    String categoryName = entry.getKey();
                    // orderIndex 기준 정렬 후 상위 3개 선택
                    List<CategoryArtworkResponse.SimpleArtworkResponse> simpleArtworks = entry.getValue().stream()
                            .sorted(Comparator.comparingInt(a -> a.getOrderIndex() != null ? a.getOrderIndex() : Integer.MAX_VALUE))
                            .limit(3)
                            .map(a -> CategoryArtworkResponse.SimpleArtworkResponse.builder()
                                    .id(a.getId())
                                    .title(a.getTitle())
                                    .imageUrl(a.getMainImg())
                                    .exhibitionTitle(exhibitionDetailMap.getOrDefault(a.getExhibition().getId(), "Unknown Exhibition"))
                                    .artistName(artistMap.getOrDefault(a.getId(), "Unknown Artist"))
                                    .build())
                            .collect(Collectors.toList());
                    return CategoryArtworkResponse.builder()
                            .categoryName(categoryName)
                            .artworks(simpleArtworks)
                            .build();
                })
                .collect(Collectors.toList());

        return MainCategoryResponse.builder()
                .categories(categoryResponses)
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public ExhibitionArtworkListResponse getExhibitionArtworkList(UUID exhibitionId, String zone, String category) {
        // 1. 전시회 존재 여부 확인
        if (!exhibitionRepository.existsById(exhibitionId)) {
            throw new RuntimeException("해당 전시회를 찾을 수 없습니다.");
        }

        // 2. 안내 지도 리스트 조회
        List<ExhibitionGuideMap> guideMaps = exhibitionGuideMapRepository.findByExhibitionId(exhibitionId);

        // 3. 작품 및 작가 상세 정보 조회 (아까 Repository에 추가한 fetch join 메서드 사용)
        List<Artwork> artworks = artworkRepository.findArtworksForList(exhibitionId, zone, category);

        // 4. DTO 변환 및 반환
        return ExhibitionArtworkListResponse.builder()
                .exhibitionId(exhibitionId)
                .maps(guideMaps.stream()
                        .map(m -> ExhibitionArtworkListResponse.MapInfo.builder()
                                .id(m.getId())
                                .imageUrl(m.getImageUrl())
                                .description(m.getDescription())
                                .build())
                        .toList())
                .artworks(artworks.stream()
                        .map(a -> ExhibitionArtworkListResponse.ArtworkInfo.builder()
                                .artworkId(a.getId())
                                .title(a.getTitle())
                                .category(a.getCategory())
                                .zone(a.getExhibitionZone() != null ? a.getExhibitionZone().getName() : null)
                                .mainImage(a.getMainImg())
                                .artists(a.getArtworkArtistMaps().stream()
                                        .map(map -> ExhibitionArtworkListResponse.ArtistInfo.builder()
                                                .id(map.getArtist().getId())
                                                .name(map.getArtist().getNameKo())
                                                .build())
                                        .toList())
                                .build())
                        .toList())
                .build();
    }
}
