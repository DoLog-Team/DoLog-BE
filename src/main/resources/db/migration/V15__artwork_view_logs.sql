-- 작품 조회수 중복 제거: 같은 방문자(visitor_id)는 작품마다 한 번만 센다.
CREATE TABLE artwork_view_logs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    artwork_id BINARY(16) NOT NULL,
    visitor_id VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_artwork_view_logs_visitor UNIQUE (artwork_id, visitor_id),
    CONSTRAINT fk_artwork_view_logs_artwork FOREIGN KEY (artwork_id) REFERENCES artworks(id) ON DELETE CASCADE
);
