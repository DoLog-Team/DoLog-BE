-- 공통 프로필 전환 전에 생성된 활성 작가 중 프로필이 없는 경우를 보정한다.
-- 기존 프로필은 유지하고, 앞으로 생성되는 작가의 프로필은 애플리케이션에서 함께 생성한다.
INSERT INTO artist_profiles (
    id,
    created_at,
    updated_at,
    deleted_at,
    name_ko,
    name_en,
    bio,
    email,
    profile_img,
    purchase_contact_url,
    view_count,
    artist_id
)
SELECT UUID_TO_BIN(UUID()),
       NOW(6),
       NOW(6),
       NULL,
       a.name_ko,
       a.name_en,
       NULL,
       account.email,
       NULL,
       NULL,
       0,
       a.id
FROM artists a
LEFT JOIN accounts account
       ON account.id = a.account_id
WHERE a.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM artist_profiles profile
      WHERE profile.artist_id = a.id
  );
