package com.dolog.server.domain.artwork.service;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artist.entity.ArtistProfile;
import com.dolog.server.domain.artist.exception.artistError.ArtistNotFoundException;
import com.dolog.server.domain.artist.exception.artistProfileError.ArtistProfileNotFoundException;
import com.dolog.server.domain.artist.repository.ArtistProfileRepository;
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
import com.dolog.server.domain.exhibition.entity.*;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionDetailRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionGuideMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
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
    private final ArtworkArtistMapRepository artworkArtistMapRepository;
    private final ExhibitionDetailRepository exhibitionDetailRepository;
    private final ExhibitionRepository exhibitionRepository;
    private final ExhibitionZoneRepository exhibitionZoneRepository;
    private final ArtworkImgRepository artworkImgRepository;
    private final ExhibitionGuideMapRepository exhibitionGuideMapRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final ArtistRepository artistRepository;
    private final FileService fileService;
    private final ArtworkImageService artworkImageService;
    private final ArtworkArtistService artworkArtistService;

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

    /**
     * 작품 상세 이미지 등록
     */
    @Override
    public ArtworkImgCreateResponse createArtworkImages(UUID artworkId, List<ArtworkImgCreateRequest> requests) {
        return artworkImageService.createArtworkImages(artworkId, requests);
    }

    /**
     * 작품 상세 이미지 수정
     */
    @Override
    public ArtworkImgUpdateResponse updateArtworkImage(UUID artworkId, UUID imageId, ArtworkImgUpdateRequest request) {
        return artworkImageService.updateArtworkImage(artworkId, imageId, request);
    }

    @Override
    public void deleteArtworkImage(UUID artworkId, UUID imageId) {
        artworkImageService.deleteArtworkImage(artworkId, imageId);
    }

    /**
     * 작품 기본 정보 수정
     */
    @Override
    @Transactional
    public ArtworkCreateResponse updateArtwork(UUID artworkId, ArtworkUpdateRequest request) {
        // 1. 작품 조회 (작품을 가져오면 그 안에 이미 Exhibition 정보가 들어있음)
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        // ✨ 수정 시 이미지 변경 사항 처리 (새 파일이 왔을 때만 업로드)
        String mainImgUrl = artwork.getMainImg();
        String locationMapUrl = artwork.getLocationMap();

        try {
            if (request.getMainImageFile() != null && !request.getMainImageFile().isEmpty()) {
                fileService.deleteFile(artwork.getMainImg()); // 기존 파일 삭제
                mainImgUrl = fileService.uploadFile(request.getMainImageFile(), "artworks/main");
            }
            if (request.getLocationMapFile() != null && !request.getLocationMapFile().isEmpty()) {
                fileService.deleteFile(artwork.getLocationMap()); // 기존 파일 삭제
                locationMapUrl = fileService.uploadFile(request.getLocationMapFile(), "artworks/maps");
            }
        } catch (IOException e) {
            throw new RuntimeException("파일 수정 중 오류 발생");
        }
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

        // 4. 정보 업데이트 (순서: title, description, category, zone, material, size, mainImgUrl, purchaseUrl)
        artwork.updateAllInfo(
                request.getTitle(),
                request.getDescription(),
                request.getCategory(),
                zone,
                request.getMaterial(),
                request.getSize(),
                mainImgUrl,
                locationMapUrl,
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

    @Override
    @Transactional
    public ArtworkUpdateFullResponse updateArtworkFull(UUID exhibitionId, UUID artworkId, ArtworkUpdateFullRequest request) {
        // 1. 작품 조회
        Artwork artwork = artworkRepository.findById(artworkId)
                .orElseThrow(() -> new ArtworkException(ArtworkErrorCode.ARTWORK_NOT_FOUND));

        // 2. 전시 구역(Zone) 조회
        ExhibitionZone exhibitionZone = exhibitionZoneRepository.findByIdAndExhibitionId(request.getZoneId(), exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.ZONE_NOT_FOUND));

        // 3. 기획안의 6개 필드 기반 업데이트
        // material, size, mainImg, purchaseUrl은 기존 값을 유지하도록 호출

        artwork.updateAllInfo(
                request.getTitle(),       // 수정됨
                request.getDescription(), // 수정됨
                request.getCategory(),    // 수정됨
                exhibitionZone,           // 수정됨(zone_id로 찾은 객체)
                artwork.getMaterial(),    // 기존 유지
                artwork.getSize(),        // 기존 유지
                artwork.getMainImg(),     // 기존 유지
                artwork.getLocationMap(),
                artwork.getPurchaseUrl()  // 기존 유지
        );

        // 4. 작가 매핑 동기화
        artwork.getArtworkArtistMaps().clear();
        List<Artist> artists = artistRepository.findAllById(request.getArtistIds());
        artists.forEach(artist -> {
            artwork.getArtworkArtistMaps().add(ArtworkArtistMap.builder()
                    .artwork(artwork).artist(artist).artistRole("Artist").build());

        });

        // 5. 상세 이미지 동기화 로직 보완

        List<UUID> existingImgIds = artwork.getArtworkImg().stream()
                .map(ArtworkImg::getId)
                .toList();// [검증] 요청 바디에 담긴 ID들이 실제로 이 작품의 이미지들인지 체크

        request.getImages().stream()
                .map(ArtworkUpdateFullRequest.ImageUpdateDto::getId)
                .filter(Objects::nonNull)
                .forEach(id -> {
                    if (!existingImgIds.contains(id)) {
                        // 남의 이미지 ID이거나 존재하지 않는 ID면 예외 발생!
                        throw new ArtworkException(ArtworkErrorCode.INVALID_ARTWORK_IMAGE);
                    }
                });

        // 요청으로 들어온 ID들을 String 세트로 변환 (비교의 정확성을 위해)
        Set<String> requestIds = request.getImages().stream()
                .map(img -> img.getId() != null ? img.getId().toString() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 1) 삭제: 요청 ID 목록에 없는 기존 이미지들만 제거

        artwork.getArtworkImg().removeIf(img -> !requestIds.contains(img.getId().toString()));

        // 2) 수정 및 추가

        request.getImages().forEach(imgDto -> {
            if (imgDto.getId() != null) {
                // 수정: 리스트에 남아있는 녀석을 찾아서 업데이트
                artwork.getArtworkImg().stream()
                        .filter(img -> img.getId().toString().equals(imgDto.getId().toString()))
                        .findFirst()
                        .ifPresent(img -> img.update(imgDto.getImageUrl(), imgDto.getDescription(), imgDto.getOrderIndex()));
            } else {

                // 추가
                artwork.getArtworkImg().add(ArtworkImg.builder()
                        .artwork(artwork)
                        .imageUrl(imgDto.getImageUrl())
                        .description(imgDto.getDescription())
                        .orderIndex(imgDto.getOrderIndex())
                        .build());
            }

        });

        artworkRepository.saveAndFlush(artwork);
        System.out.println("기존 이미지 개수: " + artwork.getArtworkImg().size());

        // 6. 결과 반환 시점에도 최신화된 리스트 사용
        return ArtworkUpdateFullResponse.of(
                artwork.getId(),
                artwork.getTitle(),
                artwork.getArtworkArtistMaps().stream().map(map -> map.getArtist().getId()).toList(),
                artwork.getArtworkImg().stream().map(ArtworkImg::getId).toList()
        );

    }
}
