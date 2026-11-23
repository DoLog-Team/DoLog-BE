CREATE TABLE maintenance_mode (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    start_at DATETIME(6) NULL,
    end_at DATETIME(6) NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6)
);

INSERT INTO maintenance_mode (id, is_active, created_at, updated_at) VALUES (1, FALSE, NOW(6), NOW(6));
