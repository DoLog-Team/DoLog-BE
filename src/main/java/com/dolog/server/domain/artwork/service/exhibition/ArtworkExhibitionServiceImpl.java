package com.dolog.server.domain.artwork.service.exhibition;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.web.dto.response.CategoryArtworkResponse;
import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.dolog.server.domain.exhibition.entity.ExhibitionGuideMap;
import com.dolog.server.domain.exhibition.repository.ExhibitionDetailRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionGuideMapRepository;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.web.dto.response.artwork.ExhibitionArtworkListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtworkExhibitionServiceImpl implements ArtworkExhibitionService {

    private final ExhibitionRepository exhibitionRepository;
    private final ExhibitionGuideMapRepository exhibitionGuideMapRepository;
    private final ExhibitionDetailRepository exhibitionDetailRepository;
    private final ArtworkRepository artworkRepository; // 작품 조회를 위해 필요

    @Override
    public ExhibitionArtworkListResponse getExhibitionArtworkList(UUID exhibitionId, String zone, String category, String search) {
        // 1. 전시회 존재 여부 확인
        if (!exhibitionRepository.existsById(exhibitionId)) {
            throw new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND);
        }

        // 2. 안내 지도 리스트 조회
        List<ExhibitionGuideMap> guideMaps = exhibitionGuideMapRepository.findByExhibitionId(exhibitionId);

        // 3. 작품 조회 로직 분기 (검색어 여부에 따라 다른 Repository 메서드 호출)
        List<Artwork> artworks;
        if (search != null && !search.trim().isEmpty()) {
            // 검색어가 있으면 검색 쿼리 사용 (Repository에 findArtworksBySearch 추가 필요)
            artworks = artworkRepository.findArtworksBySearch(exhibitionId, search);
        } else {
            // 검색어가 없으면 기존 필터링 쿼리 사용
            artworks = artworkRepository.findArtworksForList(exhibitionId, zone, category);
        }

        // 4. 데이터 가공: Zone별 그룹화 -> 그 안에서 다시 Category별 그룹화
        // Optional.ofNullable을 사용하여 null 키 문제를 우회합니다.
        Map<java.util.Optional<com.dolog.server.domain.exhibition.entity.ExhibitionZone>, Map<String, List<Artwork>>> groupedData = artworks.stream()
                .collect(Collectors.groupingBy(
                        a -> java.util.Optional.ofNullable(a.getExhibitionZone()), // null을 Optional.empty()로 변환
                        Collectors.groupingBy(a -> a.getCategory() != null ? a.getCategory() : "기타")
                ));

        // 5. DTO 조립 및 정렬
        List<ExhibitionArtworkListResponse.ZoneInfo> zoneInfos = groupedData.entrySet().stream()
                .map(zoneEntry -> {
                    var ez = zoneEntry.getKey().orElse(null); // ExhibitionZone 엔티티

                    // 카테고리별 응답 리스트 생성
                    List<CategoryArtworkResponse> categoryResponses = zoneEntry.getValue().entrySet().stream()
                            .map(catEntry -> CategoryArtworkResponse.builder()
                                    .categoryName(catEntry.getKey())
                                    .artworks(catEntry.getValue().stream()
                                            .map(this::mapToSimpleArtwork) // 작품 변환 로직 분리
                                            .toList())
                                    .build())
                            .toList();

                    return ExhibitionArtworkListResponse.ZoneInfo.builder()
                            .zoneName(ez != null ? ez.getName() : "미지정 구역")
                            .description(ez != null ? ez.getDescription() : null)
                            .zoneOrderId(ez != null ? ez.getOrderId() : 999) // 순서 지정
                            .categories(categoryResponses)
                            .build();
                })
                // 구역 순서(zoneOrderId)에 따른 정렬
                .sorted(Comparator.comparingInt(z -> z.getZoneOrderId() != null ? z.getZoneOrderId() : Integer.MAX_VALUE))
                .toList();

        return ExhibitionArtworkListResponse.builder()
                .exhibitionId(exhibitionId)
                .maps(guideMaps.stream()
                        .map(m -> ExhibitionArtworkListResponse.MapInfo.builder()
                                .id(m.getId())
                                .imageUrl(m.getImageUrl())
                                .description(m.getDescription())
                                .build())
                        .toList())
                .zones(zoneInfos) // 가공된 zones 데이터 삽입
                .build();
    }

    private CategoryArtworkResponse.SimpleArtworkResponse mapToSimpleArtwork(Artwork a) {
        // 작가가 여러 명일 수 있으므로 쉼표로 연결 (1순위: ArtistProfile 이름, 2순위: Artist 기본 이름)
        String artistNames = a.getArtworkArtistMaps().stream()
                .map(aam -> {
                    if (aam.getArtistProfile() != null && aam.getArtistProfile().getNameKo() != null
                            && !aam.getArtistProfile().getNameKo().isBlank()) {
                        return aam.getArtistProfile().getNameKo();
                    }
                    return aam.getArtist().getNameKo();
                })
                .collect(Collectors.joining(", "));

        // 2. 전시회 제목 가져오기 (ExhibitionDetail이 @OneToOne이므로 바로 접근)
        String exhibitionTitle = "";
        if (a.getExhibition() != null && a.getExhibition().getExhibitionDetail() != null) {
            exhibitionTitle = a.getExhibition().getExhibitionDetail().getTitle();
        }

        return CategoryArtworkResponse.SimpleArtworkResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .imageUrl(a.getMainImg())
                .exhibitionId(a.getExhibition() != null ? a.getExhibition().getId() : null)
                .slug(a.getExhibition() != null ? a.getExhibition().getSlug() : null)
                .exhibitionTitle(exhibitionTitle)
                .artistName(artistNames)
                .build();
    }

    @Override
    public Map<UUID, String> fetchExhibitionDetailMap(List<UUID> exhibitionIds) {
        List<ExhibitionDetail> details = exhibitionDetailRepository.findByExhibitionIdIn(exhibitionIds);
        return details.stream()
                .collect(Collectors.toMap(
                        ed -> ed.getExhibition().getId(), // Key: Exhibition의 UUID
                        ExhibitionDetail::getTitle,
                        (existing, replacement) -> existing  // 동일 전시회 중복 시 첫 번째 값 유지
                ));
    }
}
