-- 관람 안내 이미지(구역 지도) 순서 필드.
-- 기존 데이터는 전시별로 등록 시각(created_at) 오름차순으로 1, 2, 3 ... 번호를 채워 현재 보이는 순서를 유지한다.
ALTER TABLE exhibition_guide_maps ADD COLUMN order_index INT NULL;

UPDATE exhibition_guide_maps g
    JOIN (
        SELECT id,
               ROW_NUMBER() OVER (PARTITION BY exhibition_id ORDER BY created_at, id) AS rn
        FROM exhibition_guide_maps
    ) t ON t.id = g.id
SET g.order_index = t.rn;
