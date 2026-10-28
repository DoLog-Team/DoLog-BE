-- 기존 영문 slug 값을 새 한글 카테고리 목록 기준으로 일괄 변환한다.
UPDATE artworks SET category = '회화' WHERE category = 'painting';
UPDATE artworks SET category = '사진' WHERE category = 'photography';
UPDATE artworks SET category = '조소,설치' WHERE category IN ('sculpture', 'installation', 'media');
UPDATE artworks SET category = '공예' WHERE category = 'craft';
UPDATE artworks SET category = '일러스트,그래픽' WHERE category = 'design';
UPDATE artworks SET category = '기타' WHERE category = 'etc';

-- 위에서 안 잡힌, 목록에 없는 나머지 값(빈 값/NULL 제외)은 기타로 둔다.
UPDATE artworks
SET category = '기타'
WHERE category IS NOT NULL
  AND TRIM(category) <> ''
  AND category NOT IN ('회화', '사진', '제품', '건축', '조소,설치', '공예', '일러스트,그래픽', '기타');
