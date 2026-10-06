-- 작품 그룹 숨김 여부 (true면 사용자 화면에서 그룹의 작품을 숨김)
ALTER TABLE exhibition_zones ADD COLUMN is_hidden BOOLEAN NOT NULL DEFAULT FALSE;
