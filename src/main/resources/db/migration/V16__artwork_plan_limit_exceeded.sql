-- 플랜 한도 초과로 인한 자동 미노출 (작가 설정 공개/비공개, 관리자 숨김과는 별개 상태)
ALTER TABLE artworks ADD COLUMN plan_limit_exceeded_at DATETIME(6) NULL;
