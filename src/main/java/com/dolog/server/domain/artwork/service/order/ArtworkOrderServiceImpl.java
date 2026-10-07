package com.dolog.server.domain.artwork.service.order;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.repository.ArtworkSpecification;
import com.dolog.server.domain.artwork.service.artist.ArtworkArtistService;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.exhibition.exception.ExhibitionErrorCode;
import com.dolog.server.domain.exhibition.exception.ExhibitionException;
import com.dolog.server.domain.exhibition.repository.ExhibitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArtworkOrderServiceImpl implements ArtworkOrderService {

    private final ArtworkRepository artworkRepository;
    private final ArtworkArtistService artworkArtistService;
    private final ExhibitionRepository exhibitionRepository;

    @Override
    @Transactional
    public void reorderArtworkIndices(UUID exhibitionId, UUID accountId, boolean isDologAdmin) {
        // 두록 어드민은 모든 전시, 전시 어드민은 본인 전시만 (작품 그룹 API 와 같은 규칙)
        Exhibition exhibition = exhibitionRepository.findById(exhibitionId)
                .orElseThrow(() -> new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_FOUND));
        if (!isDologAdmin && !exhibition.getAccount().getId().equals(accountId)) {
            throw new ExhibitionException(ExhibitionErrorCode.EXHIBITION_NOT_OWNER);
        }

        // 1. 해당 전시회의 모든 작품 조회
        Specification<Artwork> spec = Specification.where(ArtworkSpecification.withExhibitionFetch())
                .and((root, query, cb) -> cb.equal(root.get("exhibition").get("id"), exhibitionId));
        List<Artwork> artworks = artworkRepository.findAll(spec);

        if (artworks.isEmpty()) {
            return;
        }

        // 2. 정렬에 필요한 작가 Map 일괄 조회 (N+1 방지)
        List<UUID> artworkIds = artworks.stream().map(Artwork::getId).toList();
        Map<UUID, String> artistMap = artworkArtistService.fetchArtistMap(artworkIds);

        // 3. 존(ExhibitionZone)별로 작품들을 그룹화
        Map<UUID, List<Artwork>> artworksByZone = artworks.stream()
                .filter(a -> a.getExhibitionZone() != null)
                .collect(Collectors.groupingBy(a -> a.getExhibitionZone().getId()));

        // 4. 각 존 내부에서 [1순위: 작가명 가나다, 2순위: 작품명 가나다]로 정렬 후 orderIndex 갱신
        artworksByZone.forEach((zoneId, zoneArtworks) -> {
            // 정렬 수행
            zoneArtworks.sort(Comparator
                    .comparing((Artwork a) -> artistMap.getOrDefault(a.getId(), ""), Comparator.naturalOrder())
                    .thenComparing(Artwork::getTitle, Comparator.naturalOrder())
            );

            // 10부터 시작해서 10씩 증가하며 orderIndex 재부여
            int newIndex = 10;
            for (Artwork artwork : zoneArtworks) {
                artwork.updateOrder(newIndex);
                newIndex += 10;
            }
        });
    }
}