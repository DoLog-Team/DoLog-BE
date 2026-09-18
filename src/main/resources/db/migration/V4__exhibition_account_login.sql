ALTER TABLE exhibitions
    MODIFY COLUMN account_id BINARY(16) NOT NULL,
    ADD CONSTRAINT uk_exhibitions_account UNIQUE (account_id),
    ADD COLUMN entry_code VARCHAR(8) COLLATE utf8mb4_bin NULL,
    ADD COLUMN artist_join_code VARCHAR(8) COLLATE utf8mb4_bin NULL,
    ADD COLUMN entry_code_expires_at DATETIME(6) NULL,
    ADD COLUMN expires_at DATETIME(6) NULL,
    ADD CONSTRAINT uk_exhibitions_entry_code UNIQUE (entry_code),
    ADD CONSTRAINT uk_exhibitions_artist_join_code UNIQUE (artist_join_code);
