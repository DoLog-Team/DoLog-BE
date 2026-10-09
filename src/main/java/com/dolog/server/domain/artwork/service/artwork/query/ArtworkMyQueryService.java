package com.dolog.server.domain.artwork.service.artwork.query;

import com.dolog.server.domain.artist.entity.Artist;
import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.repository.ArtworkSpecification;
import com.dolog.server.domain.artwork.support.ArtworkFieldRequirement;
import com.dolog.server.domain.artwork.support.ArtworkValidator;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkMyItemResponse;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkMyListResponse;
import com.dolog.server.domain.exhibition.entity.Exhibition;
import com.dolog.server.domain.like.repository.ArtworkLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

// 마이페이지 "내 작품 목록". 로그인한 작가 본인(공동 작가 포함) 작품을 공개 여부와 무관하게 다 보여준다.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtworkMyQueryService {

    private final ArtworkRepository artworkRepository;
    private final ArtworkValidator artworkValidator;
    private final ArtworkLikeRepository artworkLikeRepository;
    private final ArtworkFieldRequirement artworkFieldRequirement;

    public ArtworkMyListResponse getMyArtworks(UUID accountId, ArtworkStatus status, int page, int size) {
        Artist artist = artworkValidator.getLoginArtist(accountId);

        Specification<Artwork> spec = Specification
                .where(ArtworkSpecification.withArtistId(artist.getId()))
                .and(ArtworkSpecification.withStatus(status));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Artwork> result = artworkRepository.findAll(spec, pageable);

        return ArtworkMyListResponse.builder()
                .artworks(result.getContent().stream().map(this::toResponse).toList())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    private ArtworkMyItemResponse toResponse(Artwork artwork) {
        long likeCount = artworkLikeRepository.countByArtworkId(artwork.getId());
        Integer completionRate = artworkFieldRequirement.completionRate(artwork);
        return ArtworkMyItemResponse.of(artwork, resolveExhibitionName(artwork), likeCount, completionRate);
    }

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
