package com.dolog.server.domain.artwork.repository;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
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

    // 두록 URL 노출 조건 (작품 숨김, 구역 숨김은 전시 URL 에서만 본다)
    public static Specification<Artwork> isPublished() {
        return (root, query, cb) ->
                cb.equal(root.get("status"), ArtworkStatus.PUBLISHED);
    }

    // 게시 전 전시의 작품은 이용자에게 보이지 않는다
    public static Specification<Artwork> inPublishedExhibition() {
        return (root, query, cb) ->
                cb.isTrue(root.get("exhibition").get("isPublic"));
    }

    public static Specification<Artwork> hasExhibition() {
        return (root, query, cb) ->
                cb.isNotNull(root.get("exhibition"));
    }

    // 두록 어드민 전체 조회용. 공개 여부/노출 상태와 무관하게 조건만 건다.
    public static Specification<Artwork> withExhibitionId(UUID exhibitionId) {
        return (root, query, cb) ->
                exhibitionId == null ? cb.conjunction() : cb.equal(root.get("exhibition").get("id"), exhibitionId);
    }

    public static Specification<Artwork> withStatus(ArtworkStatus status) {
        return (root, query, cb) ->
                status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Artwork> withHidden(Boolean hidden) {
        return (root, query, cb) -> {
            if (hidden == null) return cb.conjunction();
            return hidden ? cb.isNotNull(root.get("hiddenAt")) : cb.isNull(root.get("hiddenAt"));
        };
    }

    // 마이페이지 "내 작품"용. 공동 작가로 연결된 것도 포함한다.
    public static Specification<Artwork> withArtistId(UUID artistId) {
        return (root, query, cb) -> {
            Subquery<UUID> artworkIdSubquery = query.subquery(UUID.class);
            Root<ArtworkArtistMap> aamRoot = artworkIdSubquery.from(ArtworkArtistMap.class);
            artworkIdSubquery.select(aamRoot.<Artwork>get("artwork").<UUID>get("id"))
                    .where(cb.equal(aamRoot.get("artist").get("id"), artistId));

            return root.get("id").in(artworkIdSubquery);
        };
    }
}
