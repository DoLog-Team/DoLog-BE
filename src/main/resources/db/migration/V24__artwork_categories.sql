-- 작가 등록 폼/두록 공개 필터용 작품 카테고리 드롭다운 목록.
-- artworks.category는 그대로 자유 문자열이고, 여기 값은 FK로 강제하지 않는다.
CREATE TABLE artwork_categories (
    id BINARY(16) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    order_index INT NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6)
);

INSERT INTO artwork_categories (id, name, order_index, created_at, updated_at) VALUES
    (UUID_TO_BIN(UUID()), '회화', 1, NOW(6), NOW(6)),
    (UUID_TO_BIN(UUID()), '사진', 2, NOW(6), NOW(6)),
    (UUID_TO_BIN(UUID()), '제품', 3, NOW(6), NOW(6)),
    (UUID_TO_BIN(UUID()), '건축', 4, NOW(6), NOW(6)),
    (UUID_TO_BIN(UUID()), '조소,설치', 5, NOW(6), NOW(6)),
    (UUID_TO_BIN(UUID()), '공예', 6, NOW(6), NOW(6)),
    (UUID_TO_BIN(UUID()), '일러스트,그래픽', 7, NOW(6), NOW(6)),
    (UUID_TO_BIN(UUID()), '기타', 8, NOW(6), NOW(6));
