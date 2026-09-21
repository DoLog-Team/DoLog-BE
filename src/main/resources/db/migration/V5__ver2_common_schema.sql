-- VER2 공통 ERD: #320. Auth/Account의 V2~V4 이후 적용한다.
-- 빈 개발 DB 기준이며 운영 데이터 이관 SQL은 포함하지 않는다.
-- notifications는 type enum 확정 후 별도 마이그레이션으로 추가한다.
-- 기존 API/엔티티가 사용하는 artworks.material/size, artwork_artist_maps.artist_id는 유지한다.
-- artists.account_id NOT NULL 전환은 계정 없이 작가를 생성하는 기존 API 정리 후 적용한다.

CREATE TABLE terms_agreements (
    id BINARY(16) NOT NULL PRIMARY KEY,
    account_id BINARY(16) NOT NULL,
    terms_version VARCHAR(20) NOT NULL,
    service_terms_agreed BIT(1) NOT NULL,
    privacy_agreed BIT(1) NOT NULL,
    privacy_3rd_party_agreed BIT(1) NOT NULL,
    promotion_agreed BIT(1) NULL,
    marketing_agreed BIT(1) NULL,
    ad_email_agreed BIT(1) NULL,
    ad_kakao_agreed BIT(1) NULL,
    ad_sms_agreed BIT(1) NULL,
    agreed_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_terms_agreements_account FOREIGN KEY (account_id) REFERENCES accounts(id)
);

ALTER TABLE artist_profiles
    ADD COLUMN purchase_contact_url VARCHAR(255) NULL,
    ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0;

CREATE TABLE artist_profile_likes (
    id BINARY(16) NOT NULL PRIMARY KEY,
    artist_profile_id BINARY(16) NOT NULL,
    visitor_id VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NULL,
    CONSTRAINT uk_artist_profile_likes_visitor UNIQUE (artist_profile_id, visitor_id),
    CONSTRAINT fk_artist_profile_likes_profile FOREIGN KEY (artist_profile_id) REFERENCES artist_profiles(id)
);

ALTER TABLE exhibitions
    ADD COLUMN artist_join_code_expires_at DATETIME(6) NULL,
    ADD COLUMN published_at DATETIME(6) NULL;

ALTER TABLE exhibition_details
    ADD COLUMN open_time TIME NULL,
    ADD COLUMN close_time TIME NULL,
    ADD COLUMN operation_notice VARCHAR(255) NULL;

ALTER TABLE exhibition_artist_map
    MODIFY COLUMN status ENUM('PENDING', 'JOINED', 'DENIED', 'WITHDRAWN', 'REMOVED') NULL,
    ADD COLUMN greeting TEXT NULL;

-- 전시당 설정 1행. 초기 필수 입력/숨김 설정은 모두 false다.
CREATE TABLE exhibition_field_settings (
    id BINARY(16) NOT NULL PRIMARY KEY,
    exhibition_id BINARY(16) NOT NULL,
    required_main_img BIT(1) NOT NULL DEFAULT b'0',
    required_size BIT(1) NOT NULL DEFAULT b'0',
    required_materials BIT(1) NOT NULL DEFAULT b'0',
    required_location_map BIT(1) NOT NULL DEFAULT b'0',
    hidden_size BIT(1) NOT NULL DEFAULT b'0',
    hidden_materials BIT(1) NOT NULL DEFAULT b'0',
    hidden_location_map BIT(1) NOT NULL DEFAULT b'0',
    hidden_production_period BIT(1) NOT NULL DEFAULT b'0',
    hidden_production_year BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    CONSTRAINT uk_exhibition_field_settings_exhibition UNIQUE (exhibition_id),
    CONSTRAINT fk_exhibition_field_settings_exhibition FOREIGN KEY (exhibition_id) REFERENCES exhibitions(id)
);

ALTER TABLE artworks
    MODIFY COLUMN exhibition_id BINARY(16) NULL,
    ADD COLUMN short_intro VARCHAR(255) NULL,
    ADD COLUMN width DECIMAL(10,2) NULL,
    ADD COLUMN height DECIMAL(10,2) NULL,
    ADD COLUMN depth DECIMAL(10,2) NULL,
    ADD COLUMN production_start_year INT NULL,
    ADD COLUMN production_start_month INT NULL,
    ADD COLUMN production_start_day INT NULL,
    ADD COLUMN production_end_year INT NULL,
    ADD COLUMN production_end_month INT NULL,
    ADD COLUMN production_end_day INT NULL,
    ADD COLUMN purchase_chat_url VARCHAR(255) NULL,
    ADD COLUMN show_purchase_button BIT(1) NULL,
    -- 기존 작품 생성 API가 아직 status를 쓰지 않으므로 기본값은 DRAFT로 둔다.
    ADD COLUMN status ENUM('DRAFT', 'PUBLISHED', 'HIDDEN') NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN hidden_at DATETIME(6) NULL,
    ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0;

CREATE TABLE artwork_materials (
    id BINARY(16) NOT NULL PRIMARY KEY,
    artwork_id BINARY(16) NOT NULL,
    name VARCHAR(100) NOT NULL,
    order_index INT NULL,
    created_at DATETIME(6) NULL,
    CONSTRAINT fk_artwork_materials_artwork FOREIGN KEY (artwork_id) REFERENCES artworks(id)
);

CREATE TABLE artwork_likes (
    id BINARY(16) NOT NULL PRIMARY KEY,
    artwork_id BINARY(16) NOT NULL,
    visitor_id VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NULL,
    CONSTRAINT uk_artwork_likes_visitor UNIQUE (artwork_id, visitor_id),
    CONSTRAINT fk_artwork_likes_artwork FOREIGN KEY (artwork_id) REFERENCES artworks(id)
);

ALTER TABLE plans
    ADD COLUMN max_artwork_count INT NULL,
    ADD COLUMN min_commitment_months INT NULL,
    ADD COLUMN is_popular BIT(1) NULL DEFAULT b'0',
    ADD COLUMN display_order INT NULL,
    ALTER COLUMN is_active SET DEFAULT b'1';

CREATE TABLE plan_prices (
    id BINARY(16) NOT NULL PRIMARY KEY,
    plan_id BINARY(16) NOT NULL,
    billing_cycle ENUM('MONTHLY', 'SEMIANNUAL', 'ANNUAL') NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    discount_rate DECIMAL(5,2) NULL,
    months INT NOT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    CONSTRAINT uk_plan_prices_cycle UNIQUE (plan_id, billing_cycle),
    CONSTRAINT fk_plan_prices_plan FOREIGN KEY (plan_id) REFERENCES plans(id)
);

CREATE TABLE plan_target_sizes (
    id BINARY(16) NOT NULL PRIMARY KEY,
    plan_id BINARY(16) NOT NULL,
    target_size VARCHAR(20) NOT NULL,
    CONSTRAINT uk_plan_target_sizes_size UNIQUE (plan_id, target_size),
    CONSTRAINT fk_plan_target_sizes_plan FOREIGN KEY (plan_id) REFERENCES plans(id)
);

ALTER TABLE subscriptions
    ADD COLUMN exhibition_id BINARY(16) NOT NULL,
    ADD COLUMN billing_cycle ENUM('MONTHLY', 'SEMIANNUAL', 'ANNUAL') NOT NULL,
    ADD COLUMN months INT NOT NULL,
    MODIFY COLUMN status ENUM('ACTIVE', 'CANCELED', 'EXPIRED', 'PENDING_PAYMENT') NULL,
    ADD CONSTRAINT fk_subscriptions_exhibition FOREIGN KEY (exhibition_id) REFERENCES exhibitions(id);
