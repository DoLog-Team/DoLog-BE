package com.dolog.server.domain.artwork.service;

import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.repository.ArtworkSpecification;
import com.dolog.server.domain.artwork.web.dto.request.*;
import com.dolog.server.domain.artwork.web.dto.response.*;
import com.dolog.server.domain.exhibition.entity.*;
import com.dolog.server.domain.exhibition.repository.ExhibitionZoneRepository;
import com.dolog.server.domain.exhibition.web.dto.response.artwork.ExhibitionArtworkListResponse;
import com.dolog.server.global.util.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtworkServiceImpl implements ArtworkService {

    private final ArtworkRepository artworkRepository;
    private final ExhibitionZoneRepository exhibitionZoneRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final ArtistRepository artistRepository;
    private final FileService fileService;
    private final ArtworkImageService artworkImageService;
    private final ArtworkArtistService artworkArtistService;
    private final ArtworkExhibitionService artworkExhibitionService;
    private final ArtworkUpdateService artworkUpdateService;

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
        Map<UUID, String> artistMap = artworkArtistService.fetchArtistMap(artworkIds);
        Map<UUID, String> exhibitionDetailMap = artworkExhibitionService.fetchExhibitionDetailMap(exhibitionIds);

        // 5. main 여부에 따라 응답 형태 분기
        if (Boolean.TRUE.equals(main)) {
            return buildMainCategoryResponse(artworks, artistMap, exhibitionDetailMap);
        } else {
            return buildArtworkListResponse(artworks, artistMap, exhibitionDetailMap);
        }
    }

    // 작품 기본 정보 등록
    @Override
    public ArtworkCreateResponse createArtwork(ArtworkCreateRequest request) {
        // 1. ArtistProfile 조회 (여기서 Artist와 Exhibition 정보를 한 번에 가져옴)
        ArtistProfile profile = artistProfileRepository.findById(request.getArtistProfileId())
                .orElseThrow(() -> new RuntimeException("Artist Profile not found"));

        // ✨ S3 업로드 로직 적용
        String mainImgUrl = null;
        String locationMapUrl = null;
        try {
            mainImgUrl = fileService.uploadFile(request.getMainImageFile(), "artworks/main");
            locationMapUrl = fileService.uploadFile(request.getLocationMapFile(), "artworks/maps");
        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 중 오류가 발생했습니다.");
        }

        // ArtworkServiceImpl.java
        Artwork artwork = Artwork.builder()
                .exhibition(profile.getExhibition())
                //.exhibitionZone(exhibitionZone)
                .title(request.getTitle())
                .intro(request.getIntro())
                .category(request.getCategory())
                .material(request.getMaterial())
                .size(request.getSize())
                .description(request.getDescription())
                .mainImg(mainImgUrl)
                .locationMap(locationMapUrl)
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

    @Override
    @Transactional
    public ArtworkArtistMappingResponse createArtistMapping(UUID artworkId, ArtworkArtistMappingRequest request) {
        return artworkArtistService.createArtistMapping(artworkId, request);
    }

    @Override
    @Transactional
    public ArtworkArtistMappingResponse updateArtistMapping(UUID artworkId, UUID artistProfileId, ArtworkArtistMappingRequest request) {
        // 직접 로직을 수행하지 않고, 새로 만든 전문가(artistService)에게 일을 시킵니다.
        return artworkArtistService.updateArtistMapping(artworkId, artistProfileId, request);
    }

    @Override
    @Transactional
    public void deleteArtistMapping(UUID artworkId, UUID artistProfileId) {
        artworkArtistService.deleteArtistMapping(artworkId, artistProfileId);
    }

    // 작품 상세 이미지 등록
    @Override
    public ArtworkImgCreateResponse createArtworkImages(UUID artworkId, List<ArtworkImgCreateRequest> requests) {
        return artworkImageService.createArtworkImages(artworkId, requests);
    }

    // 작품 상세 이미지 수정
    @Override
    public ArtworkImgUpdateResponse updateArtworkImage(UUID artworkId, UUID imageId, ArtworkImgUpdateRequest request) {
        return artworkImageService.updateArtworkImage(artworkId, imageId, request);
    }

    @Override
    public void deleteArtworkImage(UUID artworkId, UUID imageId) {
        artworkImageService.deleteArtworkImage(artworkId, imageId);
    }

    // 작품 기본 정보 수정
    @Override
    @Transactional
    public ArtworkCreateResponse updateArtwork(UUID artworkId, ArtworkUpdateRequest request) {
        return artworkUpdateService.updateArtwork(artworkId, request);
    }

    // 작품 삭제 (추가)
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
                            .exhibitionId(exId)
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
        return artworkExhibitionService.getExhibitionArtworkList(exhibitionId, zone, category);
    }

    @Override
    @Transactional
    public ArtworkUpdateFullResponse updateArtworkFull(UUID exhibitionId, UUID artworkId, ArtworkUpdateFullRequest request) {
        return artworkUpdateService.updateArtworkFull(exhibitionId, artworkId, request);
    }
}
