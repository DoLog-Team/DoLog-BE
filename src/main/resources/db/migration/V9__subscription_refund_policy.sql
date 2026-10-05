-- 구독 결제금액 스냅샷 (신청 시점 price × (1 - discountRate/100))
ALTER TABLE subscriptions
    ADD COLUMN paid_amount DECIMAL(10, 2) NULL;

-- 두록 어드민이 바꿀 수 있는 시스템 설정 (key-value)
CREATE TABLE system_settings (
    setting_key VARCHAR(100) NOT NULL PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL
);

-- 구독 해지 유예기간 기본값 (일)
INSERT INTO system_settings (setting_key, setting_value) VALUES ('SUBSCRIPTION_CANCEL_GRACE_DAYS', '0');

-- 구독 해지 시 발생한 환불 기록 (실제 송금은 두록 어드민이 수동 처리 후 COMPLETED로 변경)
CREATE TABLE subscription_refunds (
    id BINARY(16) NOT NULL PRIMARY KEY,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    subscription_id BINARY(16) NOT NULL,
    exhibition_id BINARY(16) NOT NULL,
    paid_amount DECIMAL(10, 2) NOT NULL,
    total_days INT NOT NULL,
    used_days INT NOT NULL,
    refund_amount DECIMAL(10, 2) NOT NULL,
    status ENUM('PENDING', 'COMPLETED') NOT NULL,
    requested_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6) NULL,
    CONSTRAINT fk_subscription_refunds_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions(id),
    CONSTRAINT fk_subscription_refunds_exhibition FOREIGN KEY (exhibition_id) REFERENCES exhibitions(id)
);
