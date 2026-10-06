-- 알림 문구는 프론트에서 만든다. 백엔드는 종류(type)와 값(payload)만 저장
ALTER TABLE notifications DROP COLUMN message;
ALTER TABLE notifications ADD COLUMN payload JSON NULL;
