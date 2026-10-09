-- 전시 필수 항목 설정 때문에 시스템이 자동으로 비공개(DRAFT) 처리했는지 구분하는 값.
-- 작가가 직접 공개/비공개를 바꾸면(changeStatus) null로 돌아간다.
-- null이 아닌 작품만, 설정이 완화되면 시스템이 자동으로 다시 공개한다.
ALTER TABLE artworks ADD COLUMN auto_drafted_at DATETIME(6) NULL;
