-- 알림 (작가/전시 어드민 공용). 수신자는 account 기준
CREATE TABLE notifications (
    id BINARY(16) NOT NULL PRIMARY KEY,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    account_id BINARY(16) NOT NULL,
    type VARCHAR(50) NOT NULL,
    message VARCHAR(255) NOT NULL,
    reference_id BINARY(16) NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_notifications_account FOREIGN KEY (account_id) REFERENCES accounts(id),
    INDEX idx_notifications_account_created (account_id, created_at)
);
