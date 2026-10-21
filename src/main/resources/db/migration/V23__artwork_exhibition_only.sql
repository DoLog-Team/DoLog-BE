-- 전시에서만 공개. true면 출품된 작품이어도 두록 플랫폼(개인 페이지)에는 노출되지 않고 전시에서만 보인다.
ALTER TABLE artworks ADD COLUMN exhibition_only BOOLEAN NOT NULL DEFAULT FALSE;
