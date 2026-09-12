ALTER TABLE accounts
    MODIFY COLUMN email VARCHAR(255) NULL,
    MODIFY COLUMN password VARCHAR(255) NULL,
    MODIFY COLUMN role ENUM('ADMIN', 'ARTIST', 'DEVELOPER',
        'DOLOG_ADMIN', 'EXHIBITION_ADMIN', 'ARTIST_ADMIN') NOT NULL,
    ADD COLUMN social_provider_id VARCHAR(255) COLLATE utf8mb4_bin NULL,
    ADD CONSTRAINT uk_accounts_social_identity UNIQUE (social_provider, social_provider_id);

ALTER TABLE refresh_token
    DROP INDEX UK6t7skxndr9jtm3ckw71g75tfl,
    MODIFY COLUMN email VARCHAR(255) NULL,
    MODIFY COLUMN token VARCHAR(1024) NOT NULL,
    ADD COLUMN account_id BINARY(16) NULL,
    ADD CONSTRAINT fk_refresh_token_account FOREIGN KEY (account_id) REFERENCES accounts(id);
