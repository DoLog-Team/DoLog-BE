package com.dolog.server.domain.artwork.service.artwork.query;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.repository.ArtworkRepository;
import com.dolog.server.domain.artwork.repository.ArtworkSpecification;
import com.dolog.server.domain.artwork.service.artwork.query.mapper.ArtworkResponseMapper;
import com.dolog.server.domain.artwork.support.ArtworkMetaLoader;
import com.dolog.server.domain.artwork.support.ArtworkSortProvider;
import com.dolog.server.domain.artwork.web.dto.response.ArtworkListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtworkQueryService {

    private final ArtworkRepository artworkRepository;
    private final ArtworkMetaLoader artworkMetaLoader;
    private final ArtworkResponseMapper artworkResponseMapper;
    private final ArtworkSortProvider artworkSortProvider;

    public List<ArtworkListResponse> getArtworks(
            Boolean main,
            String category,
            String search,
            String sort
    ) {

        if (Boolean.TRUE.equals(main)) {
            return getMainArtworks(category, search);
        }

        return getNormalArtworks(category, search, sort);
    }

    /*
     * =========================================================
     * Main Artwork
     * =========================================================
     */

    private List<ArtworkListResponse> getMainArtworks(
            String category,
            String search
    ) {

        Specification<Artwork> spec = Specification
                .where(ArtworkSpecification.withCategory(category))
                .and(ArtworkSpecification.withSearch(search));

        long totalCount = artworkRepository.count(spec);

        if (totalCount == 0) {
            return Collections.emptyList();
        }

        int size = 4;

        int maxOffset = Math.max((int) totalCount - size, 0);

        int randomOffset = maxOffset > 0
                ? ThreadLocalRandom.current().nextInt(maxOffset + 1)
                : 0;

        Pageable pageable = PageRequest.of(
                randomOffset / size,
                size
        );

        List<Artwork> artworks = artworkRepository
                .findAll(spec, pageable)
                .getContent();

        var meta = artworkMetaLoader.load(artworks);

        return artworkResponseMapper.toArtworkListResponses(
                artworks,
                meta.artistMap(),
                meta.exhibitionMap()
        );
    }

    /*
     * =========================================================
     * Artwork List
     * =========================================================
     */

    private List<ArtworkListResponse> getNormalArtworks(
            String category,
            String search,
            String sort
    ) {

        Specification<Artwork> spec = Specification
                .where(ArtworkSpecification.withExhibitionFetch())
                .and(ArtworkSpecification.withCategory(category))
                .and(ArtworkSpecification.withSearch(search));

        List<Artwork> artworks = artworkRepository.findAll(
                spec,
                artworkSortProvider.getSort(sort)
        );

        if (artworks.isEmpty()) {
            return Collections.emptyList();
        }

        var meta = artworkMetaLoader.load(artworks);

        return artworkResponseMapper.toArtworkListResponses(
                artworks,
                meta.artistMap(),
                meta.exhibitionMap()
        );
    }
}