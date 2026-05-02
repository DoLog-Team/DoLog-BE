package com.dolog.server.domain.artwork.service.artwork;

import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
import com.dolog.server.domain.artist.repository.ArtistRepository;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.exception.ArtworkErrorCode;
import com.dolog.server.domain.artwork.exception.ArtworkException;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.repository.ArtworkSpecification;
import com.dolog.server.domain.artwork.service.artist.ArtworkArtistService;
import com.dolog.server.domain.artwork.service.artworkUpdate.ArtworkUpdateService;
import com.dolog.server.domain.artwork.service.exhibition.ArtworkExhibitionService;
import com.dolog.server.domain.artwork.service.image.ArtworkImageService;
import com.dolog.server.domain.artwork.service.order.ArtworkOrderAdapter;
import com.dolog.server.domain.artwork.web.dto.request.*;
import com.dolog.server.domain.artwork.web.dto.response.*;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
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
    private final ArtworkOrderAdapter artworkOrderAdapter;

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

        // 3. 작품 ID / 전시회 ID 목록 추출
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

    /**
     * 작품 기본 정보 등록
     */

    @Override
    public ArtworkCreateResponse createArtwork(ArtworkCreateRequest request) {
        // 1. ArtistProfile 조회
        ArtistProfile profile = artistProfileRepository.findById(request.getArtistProfileId())
                .orElseThrow(() -> new RuntimeException("Artist Profile not found"));


        // 2. Zone 조회
        ExhibitionZone exhibitionZone;

        if (request.getZoneId() != null) {
            exhibitionZone = exhibitionZoneRepository
                    .findByIdAndExhibitionId(
                            request.getZoneId(),
                            profile.getExhibition().getId()
                    )
                    .orElseThrow(() -> new RuntimeException("해당 전시에 속한 Zone이 아닙니다."));
        } else {
            throw new RuntimeException("Zone은 필수입니다.");
        }


        // 4. S3 업로드
        String mainImgUrl = null;
        String locationMapUrl = null;
        try {
            mainImgUrl = fileService.uploadFile(request.getMainImageFile(), "artworks/main");
            locationMapUrl = fileService.uploadFile(request.getLocationMapFile(), "artworks/maps");
        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 중 오류가 발생했습니다.");
        }

        Artwork artwork = Artwork.builder()
                .exhibition(profile.getExhibition())
                .exhibitionZone(exhibitionZone)
                .title(request.getTitle())
                .category(request.getCategory())
                .material(request.getMaterial())
                .size(request.getSize())
                .description(request.getDescription())
                .mainImg(mainImgUrl)
                .locationMap(locationMapUrl)
                .youtubeUrl(request.getYoutubeUrl())
                .purchaseUrl(request.getPurchaseUrl())
                .build();

        artworkOrderAdapter.assign(artwork);

        // 6. 작가 매핑
        ArtworkArtistMap artistMap = ArtworkArtistMap.builder()
                .artwork(artwork)
                .artist(profile.getArtist())
                .artistProfile(profile)
                .artistRole(request.getArtistRole() != null ? request.getArtistRole() : "Artist")
                .build();

        artwork.getArtworkArtistMaps().add(artistMap);

        // 7. 저장
        Artwork saved = artworkRepository.save(artwork);

        return ArtworkCreateResponse.of(saved, profile.getNameKo());
    }

    /**
     * 작품 기본정보 수정
     */
    @Override
    @Transactional
    public ArtworkCreateResponse updateArtwork(UUID artworkId, ArtworkUpdateRequest request) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        // 3. 업데이트
        return artworkUpdateService.updateArtwork(artworkId, request);
    }


    /**
     * 작품 삭제
     */
    @Override
    @Transactional
    public void deleteArtwork(UUID artworkId) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        artworkRepository.delete(artwork);
    }



    /**
     * 같은 zone 내 reorder
     */
    @Override
    public void reorderArtwork(UUID artworkId, Integer prev, Integer next) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        artworkOrderAdapter.reorder(artwork, prev, next);
    }

    /**
     * zone 이동 + reorder
     */
    @Override
    public void moveArtworkZone(UUID artworkId, UUID zoneId, Integer prev, Integer next) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        artworkOrderAdapter.moveZone(artwork, zoneId, prev, next);
    }

    @Override
    @Transactional
    public ArtworkArtistMappingResponse createArtistMapping(UUID artworkId, ArtworkArtistMappingRequest request) {
        return artworkArtistService.createArtistMapping(artworkId, request);
    }

    @Override
    @Transactional
    public ArtworkArtistMappingResponse updateArtistMapping(UUID artworkId, UUID artistProfileId, ArtworkArtistMappingRequest request) {
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

    /**
     * 작품 목록을 flat 배열 형태의 응답으로 변환 (main=false)
     */
    private List<ArtworkListResponse> buildArtworkListResponse(
            List<Artwork> artworks,
            Map<UUID, String> artistMap,
            Map<UUID, String> exhibitionDetailMap
    ) {
        return artworks.stream()
                .sorted(Comparator
                        .comparing(
                                (Artwork a) -> a.getExhibitionZone() != null ? a.getExhibitionZone().getId() : null,
                                Comparator.nullsLast(Comparator.naturalOrder())
                        )
                        .thenComparing(a -> a.getOrderIndex() != null ? a.getOrderIndex() : Integer.MAX_VALUE)
                )
                .map(a -> {
                    UUID exId = a.getExhibition().getId();
                    String exTitle = exhibitionDetailMap.getOrDefault(exId, "Unknown Exhibition");

                    return ArtworkListResponse.builder()
                            .id(a.getId())
                            .title(a.getTitle())
                            .category(a.getCategory())
                            .imageUrl(a.getMainImg())

                            .exhibitionId(exId)
                            .exhibitionTitle(exTitle)
                            .artistName(artistMap.getOrDefault(a.getId(), "Unknown Artist"))

                            .zoneId(a.getExhibitionZone() != null ? a.getExhibitionZone().getId() : null)
                            .zoneName(a.getExhibitionZone() != null ? a.getExhibitionZone().getName() : null)
                            .orderIndex(a.getOrderIndex())

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
    public ExhibitionArtworkListResponse getExhibitionArtworkList(UUID exhibitionId, String zone, String category, String search) {
        return artworkExhibitionService.getExhibitionArtworkList(exhibitionId, zone, category, search);
    }

    @Override
    @Transactional
    public ArtworkUpdateFullResponse updateArtworkFull(UUID exhibitionId, UUID artworkId, ArtworkUpdateFullRequest request) {
        return artworkUpdateService.updateArtworkFull(exhibitionId, artworkId, request);
    }
}
