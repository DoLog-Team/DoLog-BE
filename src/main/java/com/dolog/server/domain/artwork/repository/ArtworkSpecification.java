package com.dolog.server.domain.artwork.repository;

import com.dolog.server.domain.artwork.entity.Artwork;
import com.dolog.server.domain.artwork.entity.ArtworkArtistMap;
import com.dolog.server.domain.artwork.entity.ExhibitionFieldSettings;
import com.dolog.server.domain.artwork.entity.enums.ArtworkStatus;
import com.dolog.server.domain.artwork.entity.enums.ArtworkVisibility;
import com.dolog.server.domain.exhibition.entity.ExhibitionZone;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
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
                            aamRoot.join("artist", JoinType.INNER)
                                    .join("profile", JoinType.INNER).<String>get("nameKo"),
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

    // unassigned 면 그룹 미지정 작품만
    public static Specification<Artwork> withZone(UUID zoneId, boolean unassigned) {
        return (root, query, cb) -> {
            if (unassigned) return cb.isNull(root.get("exhibitionZone"));
            if (zoneId == null) return cb.conjunction();
            return cb.equal(root.get("exhibitionZone").get("id"), zoneId);
        };
    }

    // 진행도 판정은 ArtworkFieldRequirement.isSatisfied 와 같아야 한다 (ArtworkManageApiTests 일치 테스트)
    public static Specification<Artwork> withVisibility(ArtworkVisibility visibility, ExhibitionFieldSettings settings) {
        return (root, query, cb) -> {
            if (visibility == null) return cb.conjunction();
            if (visibility == ArtworkVisibility.PUBLIC) {
                return cb.equal(root.get("status"), ArtworkStatus.PUBLISHED);
            }

            Predicate draft = cb.equal(root.get("status"), ArtworkStatus.DRAFT);
            Predicate complete = cb.and(requiredFieldsFilled(root, cb, settings).toArray(new Predicate[0]));
            return visibility == ArtworkVisibility.PRIVATE
                    ? cb.and(draft, complete)
                    : cb.and(draft, cb.not(complete));
        };
    }

    private static List<Predicate> requiredFieldsFilled(
            Root<Artwork> root, CriteriaBuilder cb, ExhibitionFieldSettings settings) {
        List<Predicate> filled = new ArrayList<>();
        // 작품 기본/상세 고정 필수값 (전시 설정과 무관하게 항상 본다)
        filled.add(notBlank(cb, root.get("category")));
        filled.add(cb.isNotNull(root.get("productionEndYear")));
        if (settings.isRequiredMainImg()) {
            filled.add(notBlank(cb, root.get("mainImg")));
        }
        if (settings.isRequiredSize()) {
            filled.add(cb.and(cb.isNotNull(root.get("width")), cb.isNotNull(root.get("height"))));
        }
        if (settings.isRequiredMaterials()) {
            filled.add(cb.isNotEmpty(root.get("materials")));
        }
        if (settings.isRequiredLocationMap()) {
            filled.add(notBlank(cb, root.get("locationMap")));
        }
        return filled;
    }

    // TEXT 컬럼이라 trim 대신 길이로 본다
    private static Predicate notBlank(CriteriaBuilder cb, Expression<String> value) {
        return cb.and(cb.isNotNull(value), cb.greaterThan(cb.length(value), 0));
    }

    // 그룹 순서 → 그룹 안 순서, 그룹 미지정은 맨 뒤
    public static Specification<Artwork> orderedLikeExhibition() {
        return (root, query, cb) -> {
            if (!Long.class.equals(query.getResultType())) {
                Join<Artwork, ExhibitionZone> zone = root.join("exhibitionZone", JoinType.LEFT);
                query.orderBy(
                        cb.asc(cb.selectCase().when(cb.isNull(zone.get("id")), 1).otherwise(0)),
                        cb.asc(zone.get("orderId")),
                        cb.asc(root.get("orderIndex")),
                        cb.asc(root.get("createdAt"))
                );
            }
            return cb.conjunction();
        };
    }
}
