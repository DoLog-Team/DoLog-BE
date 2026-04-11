package com.dolog.server.domain.artwork.service;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.exception.artistError.ArtistNotFoundException;
import com.dolog.server.domain.artist.repository.ArtistRepository;
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
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionDetailRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionZoneRepository;
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
    private final ArtistRepository artistRepository;

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
    public ArtworkCreateResponse createArtwork(UUID exhibitionId, ArtworkCreateRequest request) {
        // 전시회 존재 여부 확인
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        // zoneId가 있으면 zone 조회 (선택값)
        ExhibitionZone zone = null;
        if (request.getZoneId() != null) {
            zone = exhibitionZoneRepository.findById(request.getZoneId()).orElse(null);
        }

        Artwork artwork = Artwork.builder()
                .exhibition(exhibition)
                .exhibitionZone(zone)
                .title(request.getTitle())
                .category(request.getCategory())
                .material(request.getMaterial())
                .size(request.getSize())
                .description(request.getDescription())
                .mainImg(request.getMainImage())
                .purchaseUrl(request.getPurchaseUrl())
                .orderIndex(request.getOrderIndex())
                .build();

        Artwork saved = artworkRepository.save(artwork);
        return ArtworkCreateResponse.from(saved.getId());
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
    public ArtworkCreateResponse updateArtwork(UUID exhibitionId, UUID artworkId, ArtworkUpdateRequest request) {
        // 1. 작품 조회
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        // 2. 검증: 요청된 전시회 ID와 작품이 속한 전시회 ID가 일치하는지 확인
        if (!artwork.getExhibition().getId().equals(exhibitionId)) {
            throw new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND);
        }

        // 3. 전시 구역(Zone) 변경이 있는 경우 조회
        ExhibitionZone zone = null;
        if (request.getZoneId() != null) {
            zone = exhibitionZoneRepository.findById(request.getZoneId())
                    .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));
        }

        // 4. 엔티티의 업데이트 메서드 호출
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

        return ArtworkCreateResponse.from(artwork.getId());
    }

    /**
     * 작품 삭제 (추가)
     */
    @Override
    @Transactional
    public void deleteArtwork(UUID exhibitionId, UUID artworkId) {
        // 1. 존재 확인 및 전시회 매칭 검증
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));

        if (!artwork.getExhibition().getId().equals(exhibitionId)) {
            throw new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND);
        }

        // 2. 삭제 실행 (Artwork 엔티티에 Cascade 설정이 되어 있어야 상세 이미지도 같이 지워집니다)
        artworkRepository.delete(artwork);
    }

    // 등록 (POST)
    @Override
    @Transactional
    public ArtworkArtistMappingResponse createArtistMapping(UUID artworkId, ArtworkArtistMappingRequest request) {
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        Artist artist = artistRepository.findById(request.getArtistId())
                .orElseThrow(() -> new ArtistNotFoundException()); // 기존 예외 활용

        if (artworkArtistMapRepository.existsByArtworkIdAndArtistId(artworkId, artist.getId())) {
            throw new RuntimeException("이미 등록된 작가입니다.");
        }

        ArtworkArtistMap map = ArtworkArtistMap.builder()
                .artwork(artwork)
                .artist(artist)
                .artistRole(request.getArtistRole())
                .build();

        return ArtworkArtistMappingResponse.from(artworkArtistMapRepository.save(map).getId());
    }

    // 수정 (PATCH) - 역할(Role)만 변경
    @Override
    @Transactional
    public ArtworkArtistMappingResponse updateArtistMapping(UUID artworkId, UUID artistId, ArtworkArtistMappingRequest request) {
        // artworkId와 artistId 조합으로 매핑 데이터를 찾습니다.
        ArtworkArtistMap map = artworkArtistMapRepository.findByArtworkIdAndArtistId(artworkId, artistId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND)); // 또는 적절한 매핑 없음 에러

        map.updateRole(request.getArtistRole());

        // 응답 DTO도 기획안 형식(id, role 포함)에 맞춰서 반환하도록 설계해야 합니다.
        return ArtworkArtistMappingResponse.of(map);
    }

    // 삭제 (DELETE)
    @Override
    @Transactional
    public void deleteArtistMapping(UUID artworkId, UUID artistId) {
        ArtworkArtistMap map = artworkArtistMapRepository.findByArtworkIdAndArtistId(artworkId, artistId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        artworkArtistMapRepository.delete(map);
    }

    /**
     * 작품 ID 목록으로 작품별 작가명을 Map으로 반환
     * - 작가가 여러 명이면 ", "로 합쳐서 반환 (ex. "홍길동, 김철수")
     * - key: artworkId, value: 작가명
     */
    private Map<UUID, String> fetchArtistMap(List<UUID> artworkIds) {
        List<ArtworkArtistMap> artistMaps = artworkArtistMapRepository.findByArtworkIdIn(artworkIds);
        return artistMaps.stream()
                .collect(Collectors.groupingBy(
                        aam -> aam.getArtwork().getId(),
                        Collectors.mapping(aam -> aam.getArtist().getNameKo(), Collectors.joining(", "))
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
                        ed -> ed.getExhibition().getId(),
                        ExhibitionDetail::getTitle,
                        (existing, replacement) -> existing  // 동일 전시회 중복 시 첫 번째 값 유지
                ));
    }

    /**
     * 작품 목록을 flat 배열 형태의 응답으로 변환 (main=false)
     */
    private List<ArtworkListResponse> buildArtworkListResponse(List<Artwork> artworks, Map<UUID, String> artistMap, Map<UUID, String> exhibitionDetailMap) {
        return artworks.stream()
                .map(a -> ArtworkListResponse.builder()
                        .id(a.getId())
                        .title(a.getTitle())
                        .category(a.getCategory())
                        .imageUrl(a.getMainImg())
                        .exhibitionTitle(exhibitionDetailMap.getOrDefault(a.getExhibition().getId(), "Unknown Exhibition"))
                        .artistName(artistMap.getOrDefault(a.getId(), "Unknown Artist"))
                        .build())
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
}
