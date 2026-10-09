package com.dolog.server.domain.artwork.service.artwork.query;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.repository.ArtworkSpecification;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkAdminItemResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkAdminListResponse;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// 두록 어드민 전용 작품 전체 조회. 공개 여부/숨김/플랜 한도 초과와 무관하게 다 보여준다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtworkAdminQueryService {

    private final ArtworkRepository artworkRepository;

    public ArtworkAdminListResponse getArtworks(
            UUID exhibitionId,
            ArtworkStatus status,
            Boolean hidden,
            String search,
            int page,
            int size
    ) {
        Specification<Artwork> spec = Specification
                .where(ArtworkSpecification.withExhibitionId(exhibitionId))
                .and(ArtworkSpecification.withStatus(status))
                .and(ArtworkSpecification.withHidden(hidden))
                .and(ArtworkSpecification.withSearch(search));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Artwork> result = artworkRepository.findAll(spec, pageable);

        return ArtworkAdminListResponse.builder()
                .artworks(result.getContent().stream()
                        .map(artwork -> ArtworkAdminItemResponse.from(artwork, resolveExhibitionName(artwork)))
                        .toList())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    // 전시 이름은 상세 정보의 제목을 쓰고, 없으면 slug로 대신한다. 출품 전 개인 작품은 전시가 없다.
    private String resolveExhibitionName(Artwork artwork) {
        Exhibition exhibition = artwork.getExhibition();
        if (exhibition == null) {
            return null;
        }
        return exhibition.getExhibitionDetail() != null
                ? exhibition.getExhibitionDetail().getTitle()
                : exhibition.getSlug();
    }
}
