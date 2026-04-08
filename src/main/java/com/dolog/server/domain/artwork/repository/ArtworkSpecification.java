package com.dolog.server.domain.artwork.repository;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class ArtworkSpecification {

    public static Specification<Artwork> withCategory(String category) {
        return (root, query, cb) ->
                category == null ? cb.conjunction() : cb.equal(root.get("category"), category);
    }

    public static Specification<Artwork> withSearch(String search) {
        return (root, query, cb) -> {
            if (search == null) return cb.conjunction();
            String pattern = "%" + search + "%";

            Subquery<UUID> artworkIdSubquery = query.subquery(UUID.class);
            Root<ArtworkArtistMap> aamRoot = artworkIdSubquery.from(ArtworkArtistMap.class);
            artworkIdSubquery.select(aamRoot.<Artwork>get("artwork").<UUID>get("id"))
                    .where(cb.like(
                            aamRoot.join("artist", JoinType.INNER).<String>get("nameKo"),
                            pattern
                    ));

            return cb.or(
                    cb.like(root.<String>get("title"), pattern),
                    root.get("id").in(artworkIdSubquery)
            );
        };
    }

    public static Specification<Artwork> withExhibitionFetch() {
        return (root, query, cb) -> {
            if (!Long.class.equals(query.getResultType())) {
                root.fetch("exhibition", JoinType.INNER);
                query.distinct(true);
            }
            return cb.conjunction();
        };
    }
}
