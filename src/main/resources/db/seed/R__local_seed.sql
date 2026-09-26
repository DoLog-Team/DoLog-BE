-- 로컬 개발용 시드 데이터 (local 프로파일 전용).
-- application-local.properties의 spring.flyway.locations에만 db/seed를 추가하므로 dev/prod에는 적용되지 않는다.
-- Flyway repeatable 마이그레이션이라 V1~V5 이후에 실행된다.
--   - docker compose 볼륨을 지우고 새로 올리면 빈 DB에 자동 적용된다.
--   - 이 파일을 수정하면 체크섬이 바뀌어 다시 실행되므로 모든 INSERT는 고정 ID + INSERT IGNORE로 멱등하게 둔다.
-- 전시 운영자는 POST /api/auth/exhibition/login에 entryCode=DEME2222로 로그인한다.
-- 이메일/비밀번호 로그인은 DOLOG_ADMIN 전용이다.
-- 작가 계정은 DevAuthController(POST /api/auth/dev/artist-login)의 ARTIST_1 fixture와 같은 키로 만들어 둔다.
--   -> 개발용 작가 로그인이 이 계정과 작가를 그대로 재사용하므로 시드된 프로필/작품이 바로 보인다.
-- 이미지는 외부 placeholder URL이다. S3(localstack) 업로드 경로는 시드하지 않는다.

SET @account_exhibition = UUID_TO_BIN('11111111-1111-1111-1111-111111111111');
SET @account_artist     = UUID_TO_BIN('22222222-2222-2222-2222-222222222222');
SET @artist             = UUID_TO_BIN('33333333-3333-3333-3333-333333333333');
SET @exhibition         = UUID_TO_BIN('44444444-4444-4444-4444-444444444444');
SET @exhibition_detail  = UUID_TO_BIN('44444444-4444-4444-4444-444444444445');
SET @field_settings     = UUID_TO_BIN('44444444-4444-4444-4444-444444444446');
SET @artist_map         = UUID_TO_BIN('44444444-4444-4444-4444-444444444447');
SET @zone               = UUID_TO_BIN('55555555-5555-5555-5555-555555555555');
SET @artist_profile     = UUID_TO_BIN('66666666-6666-6666-6666-666666666666');
SET @artwork_1          = UUID_TO_BIN('77777777-7777-7777-7777-777777777771');
SET @artwork_2          = UUID_TO_BIN('77777777-7777-7777-7777-777777777772');
SET @artwork_img_1      = UUID_TO_BIN('88888888-8888-8888-8888-888888888881');
SET @artwork_img_2      = UUID_TO_BIN('88888888-8888-8888-8888-888888888882');
SET @bts                = UUID_TO_BIN('99999999-9999-9999-9999-999999999999');
SET @now = NOW(6);
SET @password = '$2a$10$wIcebCYiem.UA3iNf/JCH.qJk24jdYCOEQjB4kpmuwdItm.qGcyam';

-- 계정: 전시 운영자(코드 로그인) / 작가(개발용 fixture ARTIST_1).
-- DOLOG_ADMIN은 AdminInitializer가 부팅 시 따로 만든다.
INSERT IGNORE INTO accounts (id, created_at, updated_at, account_status, email, password, role,
                             social_provider, social_provider_id)
VALUES (@account_exhibition, @now, @now, 'ACTIVE', 'test@test.com', @password, 'EXHIBITION_ADMIN',
        NULL, NULL),
       (@account_artist, @now, @now, 'ACTIVE', 'artist_1@example.test', NULL, 'ARTIST_ADMIN',
        'DEV', 'ARTIST_1');

INSERT IGNORE INTO artists (id, created_at, updated_at, name_ko, name_en, phone, account_id)
VALUES (@artist, @now, @now, '개발용 작가 ARTIST_1', 'Dev Artist 1', '01000000000', @account_artist);

-- 전시: account_id는 UNIQUE라 계정당 1개다. entry_code/artist_join_code로 로그인·참여 흐름을 확인할 수 있다.
INSERT IGNORE INTO exhibitions (id, created_at, updated_at, dept_name, is_public, slug, univ_name,
                                account_id, exhibition_type, college_name,
                                entry_code, artist_join_code, published_at)
VALUES (@exhibition, @now, @now, '시각디자인학과', 1, 'local-demo', '두록대학교',
        @account_exhibition, 'GRADUATION', '디자인대학',
        'DEME2222', 'JOIN0001', @now);

-- 이전 시드만 보정한다. 개발자가 재발급한 코드와 다른 전시는 유지한다.
UPDATE exhibitions SET entry_code = 'DEME2222', updated_at = @now
WHERE id = @exhibition AND entry_code = 'LOCAL001';

INSERT IGNORE INTO exhibition_details (id, created_at, updated_at, title, description, sort_type, theme_type,
                                       start_date, end_date, date_info, address, address_detail,
                                       email, copyright, exhibition_img, logo_img, splash_img,
                                       favicon_img, og_title, og_description, og_image,
                                       open_time, close_time, operation_notice, exhibition_id)
VALUES (@exhibition_detail, @now, @now, '로컬 데모 졸업전시', '로컬 개발용 더미 전시다.', 'ABC', 'MINIMAL_WHITE',
        '2026-09-01', '2026-09-30', '2026.09.01 - 09.30', '서울특별시 중구 세종대로 110', '1층 전시장',
        'local@test.com', '© DoLog Local',
        'https://picsum.photos/seed/dolog-exhibition/1600/900',
        'https://placehold.co/400x120/png?text=DOLOG',
        'https://picsum.photos/seed/dolog-splash/1080/1920',
        'https://placehold.co/64x64/png?text=D',
        '로컬 데모 졸업전시', '로컬 개발용 더미 전시다.',
        'https://picsum.photos/seed/dolog-exhibition/1200/630',
        '10:00:00', '18:00:00', '월요일 휴관', @exhibition);

-- 전시당 1행. 기본값(전부 false)을 그대로 쓴다.
INSERT IGNORE INTO exhibition_field_settings (id, created_at, updated_at, exhibition_id)
VALUES (@field_settings, @now, @now, @exhibition);

INSERT IGNORE INTO exhibition_artist_map (id, created_at, updated_at, status, artist_id, exhibition_id)
VALUES (@artist_map, @now, @now, 'JOINED', @artist, @exhibition);

INSERT IGNORE INTO exhibition_zones (id, created_at, updated_at, name, description, order_id, exhibition_id)
VALUES (@zone, @now, @now, 'A 구역', '1층 입구 오른쪽', 1, @exhibition);

INSERT IGNORE INTO artist_profiles (id, created_at, updated_at, name_ko, name_en, bio, email, is_public,
                                    profile_img, purchase_contact_url, artist_id, exhibition_id)
VALUES (@artist_profile, @now, @now, '개발용 작가 ARTIST_1', 'Dev Artist 1', '로컬 개발용 더미 작가 소개다.',
        'artist_1@example.test', 1,
        'https://picsum.photos/seed/dolog-artist-1/400/400',
        'https://open.kakao.com/o/local-demo', @artist, @exhibition);

-- 작품 2개. artwork_2는 QR 발급 확인용 더미다 (artworks.id를 프론트에서 그대로 쓴다).
INSERT IGNORE INTO artworks (id, created_at, updated_at, title, short_intro, description, category,
                             material, size, width, height, depth,
                             production_start_year, production_end_year,
                             main_img, location_map, purchase_url, purchase_chat_url, show_purchase_button,
                             youtube_url, order_index, status, exhibition_id, zone_id)
VALUES (@artwork_1, @now, @now, '가로 작품', '로컬 데모 작품 1', '로컬 개발용 더미 작품이다.', '회화',
        '캔버스에 유채', '1200x900mm', 120.00, 90.00, 3.00, 2025, 2026,
        'https://picsum.photos/seed/dolog-art-1/1200/900', 'A 구역 입구',
        'https://example.com/purchase/1', 'https://open.kakao.com/o/local-demo', 1,
        'https://www.youtube.com/watch?v=dQw4w9WgXcQ', 1, 'PUBLISHED', @exhibition, @zone),
       (@artwork_2, @now, @now, '세로 작품 (QR 더미)', '로컬 데모 작품 2', 'QR 발급 확인용 더미 작품이다.', '사진',
        '아카이벌 피그먼트 프린트', '900x1200mm', 90.00, 120.00, 3.00, 2026, 2026,
        'https://picsum.photos/seed/dolog-art-2/900/1200', 'A 구역 안쪽',
        NULL, NULL, 0,
        NULL, 2, 'PUBLISHED', @exhibition, @zone);

INSERT IGNORE INTO artwork_imgs (id, created_at, updated_at, image_url, description, order_index, artwork_id)
VALUES (@artwork_img_1, @now, @now, 'https://picsum.photos/seed/dolog-art-1-d1/1600/1200', '상세 이미지 1', 1, @artwork_1),
       (@artwork_img_2, @now, @now, 'https://picsum.photos/seed/dolog-art-2-d1/1200/1600', '상세 이미지 1', 1, @artwork_2);

-- auto_increment PK지만 재실행 시 중복을 막으려고 id를 고정한다.
INSERT IGNORE INTO artwork_artist_maps (id, created_at, updated_at, artist_role, artist_id, artist_profile_id, artwork_id)
VALUES (1, @now, @now, '작가', @artist, @artist_profile, @artwork_1),
       (2, @now, @now, '작가', @artist, @artist_profile, @artwork_2);

INSERT IGNORE INTO bts (id, created_at, updated_at, title, content, main_img, content_url,
                        link_label, link_url, artist_profile_id, exhibition_id)
VALUES (@bts, @now, @now, '작업 과정 기록', '로컬 개발용 더미 BTS 본문이다.',
        'https://picsum.photos/seed/dolog-bts-1/1200/900',
        'https://picsum.photos/seed/dolog-bts-1-content/1200/1600',
        '작업 노트 보기', 'https://example.com/bts/1', @artist_profile, @exhibition);

INSERT IGNORE INTO bts_artwork_map (id, created_at, updated_at, artwork_id, bts_id)
VALUES (1, @now, @now, @artwork_1, @bts);

INSERT IGNORE INTO main_banners (id, created_at, updated_at, image_url, link_url, is_visible, order_index)
VALUES (1, @now, @now, 'https://picsum.photos/seed/dolog-banner-1/1600/600', 'https://example.com/banner/1', 1, 1),
       (2, @now, @now, 'https://picsum.photos/seed/dolog-banner-2/1600/600', 'https://example.com/banner/2', 1, 2);
