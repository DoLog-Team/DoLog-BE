package com.dolog.server.domain.artwork.service;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.exhibition.entity.ExhibitionDetail;
import com.dolog.server.domain.exhibition.entity.ExhibitionGuideMap;
import com.dolog.server.domain.exhibition.repository.ExhibitionDetailRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionGuideMapRepository;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import com.dolog.server.domain.exhibition.web.dto.response.artwork.ExhibitionArtworkListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
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
