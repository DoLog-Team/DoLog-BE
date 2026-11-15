-- 두록 작가 상세 조회수는 작가와 방문자 조합마다 한 번만 센다.
CREATE TABLE artist_view_logs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    artist_id BINARY(16) NOT NULL,
    visitor_id VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_artist_view_logs_visitor UNIQUE (artist_id, visitor_id),
    CONSTRAINT fk_artist_view_logs_artist FOREIGN KEY (artist_id) REFERENCES artists(id) ON DELETE CASCADE
);
