-- 작품 사이즈 유형과 영상 길이. v1 데이터 정리용으로 먼저 추가하고, 입력은 당분간 DIMENSIONS만 받는다.
-- size_type: NULL=미입력(표시 안 함), DIMENSIONS=가로·세로·(높이), ROUND=지름(width)·(높이 depth), VARIABLE=가변크기(숫자 없음)
-- duration_seconds: 영상 길이(초). 사이즈와 독립이다(가변+영상, 일반+영상 조합이 있다).
ALTER TABLE artworks
    ADD COLUMN size_type ENUM('DIMENSIONS', 'ROUND', 'VARIABLE') NULL,
    ADD COLUMN duration_seconds INT NULL;

UPDATE artworks SET size_type = 'DIMENSIONS'
WHERE width IS NOT NULL OR height IS NOT NULL OR depth IS NOT NULL;
