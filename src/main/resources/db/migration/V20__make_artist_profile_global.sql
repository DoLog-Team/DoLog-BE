CREATE TEMPORARY TABLE artist_profile_canonical (
    artist_id BINARY(16) NOT NULL PRIMARY KEY,
    profile_id BINARY(16) NOT NULL UNIQUE
);

INSERT INTO artist_profile_canonical (artist_id, profile_id)
SELECT artist_id, id
FROM (
    SELECT artist_id,
           id,
           ROW_NUMBER() OVER (
               PARTITION BY artist_id
               ORDER BY (deleted_at IS NULL) DESC,
                        COALESCE(updated_at, created_at) DESC,
                        HEX(id) DESC
           ) AS row_number_by_artist
    FROM artist_profiles
) ranked_profiles
WHERE row_number_by_artist = 1;

CREATE TEMPORARY TABLE artist_profile_view_totals (
    artist_id BINARY(16) NOT NULL PRIMARY KEY,
    total_view_count BIGINT NOT NULL
);

INSERT INTO artist_profile_view_totals (artist_id, total_view_count)
SELECT artist_id, COALESCE(SUM(view_count), 0)
FROM artist_profiles
GROUP BY artist_id;

UPDATE artist_profiles p
JOIN artist_profile_canonical c ON c.profile_id = p.id
JOIN artist_profile_view_totals v ON v.artist_id = p.artist_id
SET p.view_count = v.total_view_count;

-- 동일 방문자가 전시별 프로필 여러 개에 좋아요한 경우 하나만 남긴다.
CREATE TEMPORARY TABLE artist_profile_like_keep (
    like_id BINARY(16) NOT NULL PRIMARY KEY,
    canonical_profile_id BINARY(16) NOT NULL
);

INSERT INTO artist_profile_like_keep (like_id, canonical_profile_id)
SELECT like_id, canonical_profile_id
FROM (
    SELECT l.id AS like_id,
           c.profile_id AS canonical_profile_id,
           ROW_NUMBER() OVER (
               PARTITION BY p.artist_id, l.visitor_id
               ORDER BY l.created_at ASC, HEX(l.id) ASC
           ) AS row_number_by_visitor
    FROM artist_profile_likes l
    JOIN artist_profiles p ON p.id = l.artist_profile_id
    JOIN artist_profile_canonical c ON c.artist_id = p.artist_id
) ranked_likes
WHERE row_number_by_visitor = 1;

DELETE l
FROM artist_profile_likes l
LEFT JOIN artist_profile_like_keep k ON k.like_id = l.id
WHERE k.like_id IS NULL;

UPDATE artist_profile_likes l
JOIN artist_profile_like_keep k ON k.like_id = l.id
SET l.artist_profile_id = k.canonical_profile_id;

UPDATE artist_sns s
JOIN artist_profiles p ON p.id = s.artist_profile_id
JOIN artist_profile_canonical c ON c.artist_id = p.artist_id
SET s.artist_profile_id = c.profile_id;

UPDATE bts b
JOIN artist_profiles p ON p.id = b.artist_profile_id
JOIN artist_profile_canonical c ON c.artist_id = p.artist_id
SET b.artist_profile_id = c.profile_id;

UPDATE artwork_artist_maps m
JOIN artist_profiles p ON p.id = m.artist_profile_id
JOIN artist_profile_canonical c ON c.artist_id = p.artist_id
SET m.artist_profile_id = c.profile_id;

DELETE p
FROM artist_profiles p
JOIN artist_profile_canonical c ON c.artist_id = p.artist_id
WHERE p.id <> c.profile_id;

ALTER TABLE artist_profiles
    DROP FOREIGN KEY FKdnikl1xm2v9an7xwvhcmx804k,
    DROP INDEX UKpnbct7kx3hjdxuwmt2ld76mj,
    DROP COLUMN exhibition_id,
    ADD CONSTRAINT uk_artist_profiles_artist UNIQUE (artist_id);

DROP TEMPORARY TABLE artist_profile_like_keep;
DROP TEMPORARY TABLE artist_profile_view_totals;
DROP TEMPORARY TABLE artist_profile_canonical;
