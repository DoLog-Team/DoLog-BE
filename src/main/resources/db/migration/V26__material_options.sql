-- 작가 등록 폼 "재료 및 기법" 자동완성용 기본 목록. 기존 프론트 하드코딩 14개를 그대로 옮긴다.
-- artwork_materials(작품별 실제 입력값)는 그대로 자유 문자열이고, 여기 값은 FK로 강제하지 않는다.
CREATE TABLE material_options (
    id BINARY(16) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    order_index INT NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6)
);

INSERT INTO material_options (id, name, order_index, created_at, updated_at) VALUES
    (UUID_TO_BIN(UUID()), '캔버스에 아크릴릭', 1, NOW(6), NOW(6)),
    (UUID_TO_BIN(UUID()), '캔버스에 유화', 2, NOW(6), NOW(6)),
    (UUID_TO_BIN(UUID()), '모델링 페이스트', 3, NOW(6), NOW(6)),
    (UUID_TO_BIN(UUID()), '쉬폰', 4, NOW(6), NOW(6)),
    (UUID_TO_BIN(UUID()), '비즈', 5, NOW(6), NOW(6)),
    (UUID_TO_BIN(UUID()), '스테인리스 스틸', 6, NOW(6), NOW(6)),
    (UUID_TO_BIN(UUID()), '혼합재료', 7, NOW(6), NOW(6)),
    (UUID_TO_BIN(UUID()), '콜라주', 8, NOW(6), NOW(6)),
    (UUID_TO_BIN(UUID()), '목재', 9, NOW(6), NOW(6)),
    (UUID_TO_BIN(UUID()), '석고', 10, NOW(6), NOW(6)),
    (UUID_TO_BIN(UUID()), '종이에 수채', 11, NOW(6), NOW(6)),
    (UUID_TO_BIN(UUID()), '디지털 프린트', 12, NOW(6), NOW(6)),
    (UUID_TO_BIN(UUID()), '실크스크린', 13, NOW(6), NOW(6)),
    (UUID_TO_BIN(UUID()), '레진', 14, NOW(6), NOW(6));
