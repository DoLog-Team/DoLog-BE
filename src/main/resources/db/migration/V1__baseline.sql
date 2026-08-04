-- Flyway baseline (V1) — 이전 시점의 운영/개발 스키마 스냅샷
-- 생성: DataGrip SQL Generator
-- 목적: ddl-auto=validate 전환의 기준. 기존 DB는 baseline-on-migrate로 이 파일을 건너뛰고,
--       새 DB(신규 Oracle prod 컨테이너)에서는 이 파일이 실행되어 스키마를 만든다.
-- 이후 스키마 변경은 V2__ 이상으로 추가할 것 (이 파일은 수정 금지).

create table accounts
(
    id              binary(16)                                not null
        primary key,
    created_at      datetime(6)                               null,
    updated_at      datetime(6)                               null,
    account_status  enum ('ACTIVE', 'SUSPENDED', 'WITHDRAWN') null,
    email           varchar(255)                              not null,
    password        varchar(255)                              not null,
    role            enum ('ADMIN', 'ARTIST', 'DEVELOPER')     not null,
    social_provider varchar(50)                               null,
    constraint UKn7ihswpy07ci568w34q0oi8he
        unique (email)
);

create table artists
(
    id         binary(16)   not null
        primary key,
    created_at datetime(6)  null,
    updated_at datetime(6)  null,
    name_en    varchar(100) null,
    name_ko    varchar(100) not null,
    phone      varchar(30)  null,
    account_id binary(16)   null,
    constraint UKmsyean3446542m3v4t051vuiw
        unique (account_id),
    constraint FKkw1jrrjywoe6nx58966boo0fp
        foreign key (account_id) references accounts (id)
);

create table contacts
(
    id               bigint auto_increment
        primary key,
    created_at       datetime(6)  null,
    updated_at       datetime(6)  null,
    consultation_url varchar(255) null,
    image_url        varchar(255) null
);

create table exhibitions
(
    id              binary(16)                        not null
        primary key,
    created_at      datetime(6)                       null,
    updated_at      datetime(6)                       null,
    dept_name       varchar(100)                      not null,
    is_public       bit                               not null,
    slug            varchar(100)                      not null,
    univ_name       varchar(100)                      not null,
    account_id      binary(16)                        null,
    exhibition_type enum ('ASSIGNMENT', 'GRADUATION') null,
    college_name    varchar(100)                      null,
    constraint UK8cbbmp4w3yop7mftl5thng9k8
        unique (slug),
    constraint FK9gitfvxaoe4et1wgcnr51mk9
        foreign key (account_id) references accounts (id)
);

create table artist_profiles
(
    id            binary(16)   not null
        primary key,
    created_at    datetime(6)  null,
    updated_at    datetime(6)  null,
    bio           text         null,
    email         varchar(255) null,
    is_public     bit          not null,
    name_en       varchar(100) null,
    name_ko       varchar(100) null,
    profile_img   varchar(255) null,
    artist_id     binary(16)   not null,
    exhibition_id binary(16)   not null,
    constraint UKpnbct7kx3hjdxuwmt2ld76mj
        unique (artist_id, exhibition_id),
    constraint FKdnikl1xm2v9an7xwvhcmx804k
        foreign key (exhibition_id) references exhibitions (id),
    constraint FKl5i88hd04ecqioku1n41k6oja
        foreign key (artist_id) references artists (id)
);

create table artist_sns
(
    id                binary(16)   not null
        primary key,
    created_at        datetime(6)  null,
    updated_at        datetime(6)  null,
    platform_name     varchar(100) not null,
    url               varchar(255) not null,
    artist_profile_id binary(16)   not null,
    constraint FK8dnff7y5e5y7yvshpuooj8a0c
        foreign key (artist_profile_id) references artist_profiles (id)
);

create table bts
(
    id                binary(16)   not null
        primary key,
    created_at        datetime(6)  null,
    updated_at        datetime(6)  null,
    content_url       text         null,
    main_img          text         null,
    title             varchar(255) null,
    artist_profile_id binary(16)   null,
    exhibition_id     binary(16)   null,
    content           text         null,
    link_label        varchar(255) null,
    link_url          text         null,
    constraint FKar6v8cwdn8b0ix3l5hl3yupj2
        foreign key (artist_profile_id) references artist_profiles (id),
    constraint FKpv0kb68xulx9hfjybkr18jso1
        foreign key (exhibition_id) references exhibitions (id)
);

create table exhibition_artist_map
(
    id            binary(16)                           not null
        primary key,
    created_at    datetime(6)                          null,
    updated_at    datetime(6)                          null,
    status        enum ('DENIED', 'JOINED', 'PENDING') null,
    artist_id     binary(16)                           null,
    exhibition_id binary(16)                           null,
    constraint UKdvqyqs5f0ork5gvc40g9m9uxi
        unique (exhibition_id, artist_id),
    constraint FKcnxncigo594j0n1xh4udew2im
        foreign key (artist_id) references artists (id),
    constraint FKhf4ewbxtg2mt3k11axwilbcc6
        foreign key (exhibition_id) references exhibitions (id)
);

create table exhibition_custom_themes
(
    id            binary(16)   not null
        primary key,
    created_at    datetime(6)  null,
    updated_at    datetime(6)  null,
    btn_bg        varchar(20)  null,
    btn_text      varchar(20)  null,
    cta_bg        varchar(20)  null,
    cta_text      varchar(20)  null,
    theme_mode    varchar(255) not null,
    exhibition_id binary(16)   not null,
    constraint UK3g2mvv7h4hugeac94uqy3n8hm
        unique (exhibition_id),
    constraint FKomblo1gtuadx8pi6o2otb4fbl
        foreign key (exhibition_id) references exhibitions (id)
);

create table exhibition_details
(
    id                   binary(16)                            not null
        primary key,
    created_at           datetime(6)                           null,
    updated_at           datetime(6)                           null,
    address              text                                  null,
    address_detail       text                                  null,
    copyright            varchar(255)                          null,
    date_info            text                                  null,
    description          text                                  null,
    email                varchar(255)                          null,
    end_date             date                                  null,
    exhibition_img       varchar(255)                          null,
    location_description text                                  null,
    logo_img             varchar(255)                          null,
    sort_type            enum ('ABC', 'RANDOM')                not null,
    splash_img           varchar(255)                          null,
    start_date           date                                  null,
    theme_type           enum ('MINIMAL_WHITE', 'MODERN_DARK') null,
    title                varchar(255)                          null,
    exhibition_id        binary(16)                            not null,
    og_image             varchar(255)                          null,
    favicon_img          varchar(255)                          null,
    og_description       text                                  null,
    og_title             varchar(255)                          null,
    constraint UKmv8o8kwfu9fotj4fylq3yc8e6
        unique (exhibition_id),
    constraint FK5sjw8eixpth5jvrf71i539982
        foreign key (exhibition_id) references exhibitions (id)
);

create table exhibition_maps
(
    id                   binary(16)   not null
        primary key,
    created_at           datetime(6)  null,
    updated_at           datetime(6)  null,
    address              varchar(255) not null,
    detail_location      varchar(255) null,
    latitude             varchar(255) not null,
    longitude            varchar(255) not null,
    exhibition_id        binary(16)   not null,
    location_description text         null,
    constraint UK18wjs8w6reocrtd7o12jm07kr
        unique (exhibition_id),
    constraint FK1jludi49qh70c3klh10exfhr1
        foreign key (exhibition_id) references exhibitions (id)
);

create table exhibition_zones
(
    id            binary(16)   not null
        primary key,
    created_at    datetime(6)  null,
    updated_at    datetime(6)  null,
    description   varchar(255) null,
    name          varchar(100) null,
    order_id      int          null,
    exhibition_id binary(16)   null,
    constraint FK2mqmen4nssx0qf4cahd1n6930
        foreign key (exhibition_id) references exhibitions (id)
);

create table artworks
(
    id            binary(16)    not null
        primary key,
    created_at    datetime(6)   null,
    updated_at    datetime(6)   null,
    category      varchar(100)  null,
    description   text          null,
    location_map  varchar(700)  null,
    main_img      text          null,
    material      varchar(1000) null,
    order_index   int           null,
    purchase_url  varchar(255)  null,
    size          varchar(255)  null,
    title         varchar(255)  null,
    youtube_url   varchar(255)  null,
    exhibition_id binary(16)    not null,
    zone_id       binary(16)    null,
    constraint FKbavvwg1422ggjepn3wwjtt4yw
        foreign key (zone_id) references exhibition_zones (id),
    constraint FKfhj03rr9s9r048y0f7vssejbf
        foreign key (exhibition_id) references exhibitions (id)
);

create table artwork_artist_maps
(
    id                bigint auto_increment
        primary key,
    created_at        datetime(6)  null,
    updated_at        datetime(6)  null,
    artist_role       varchar(100) not null,
    artist_id         binary(16)   not null,
    artist_profile_id binary(16)   null,
    artwork_id        binary(16)   not null,
    constraint FK7ot5oeinmcibbnhehx02g646d
        foreign key (artist_id) references artists (id),
    constraint FKeni8mbx405j48mavboixbda91
        foreign key (artwork_id) references artworks (id),
    constraint FKf8b2td5v3bilf2f8imn9gxmwg
        foreign key (artist_profile_id) references artist_profiles (id)
);

create table artwork_imgs
(
    id          binary(16)  not null
        primary key,
    created_at  datetime(6) null,
    updated_at  datetime(6) null,
    description text        null,
    image_url   text        null,
    order_index int         null,
    artwork_id  binary(16)  not null,
    constraint FK54qnro49rh2jbtan4e4x2h5ny
        foreign key (artwork_id) references artworks (id)
);

create table bts_artwork_map
(
    id         bigint auto_increment
        primary key,
    created_at datetime(6) null,
    updated_at datetime(6) null,
    artwork_id binary(16)  null,
    bts_id     binary(16)  null,
    constraint FKp48aos0uns0ybf3y4vwlikavr
        foreign key (artwork_id) references artworks (id),
    constraint FKs2d6md48lpyrtomu9yhr1xkix
        foreign key (bts_id) references bts (id)
);

create table exhibition_guide_maps
(
    id            binary(16)   not null
        primary key,
    created_at    datetime(6)  null,
    updated_at    datetime(6)  null,
    description   varchar(255) null,
    image_url     varchar(255) not null,
    exhibition_id binary(16)   not null,
    zone_id       binary(16)   null,
    constraint FK3j47up6osq4qpj99hhkpjpqoq
        foreign key (zone_id) references exhibition_zones (id),
    constraint FKbhwcpf62nfyksi8m84pq3jcf6
        foreign key (exhibition_id) references exhibitions (id)
);

create table hosts
(
    id            binary(16)   not null
        primary key,
    created_at    datetime(6)  null,
    updated_at    datetime(6)  null,
    description   text         null,
    email         varchar(255) null,
    img           varchar(255) null,
    name          varchar(100) not null,
    exhibition_id binary(16)   null,
    constraint UKekere35i6smpcw8d12vtenn6x
        unique (exhibition_id),
    constraint FK2j5yonmghl5g42bp88hopn8qh
        foreign key (exhibition_id) references exhibitions (id)
);

create table host_sns
(
    id            binary(16)   not null
        primary key,
    created_at    datetime(6)  null,
    updated_at    datetime(6)  null,
    platform_name varchar(100) null,
    url           varchar(255) null,
    host_id       binary(16)   null,
    constraint FKg79hxyfhxxqon7gyn7f30ry2m
        foreign key (host_id) references hosts (id)
);

create table main_banners
(
    id          bigint auto_increment
        primary key,
    created_at  datetime(6)  null,
    updated_at  datetime(6)  null,
    image_url   varchar(255) null,
    is_visible  bit          null,
    link_url    varchar(255) null,
    order_index int          null
);

create table partners
(
    id            binary(16)   not null
        primary key,
    created_at    datetime(6)  null,
    updated_at    datetime(6)  null,
    description   varchar(255) null,
    name          varchar(100) null,
    display_order int          null,
    exhibition_id binary(16)   not null,
    constraint FKebx4j5kr823ivfxiqy3fuduya
        foreign key (exhibition_id) references exhibitions (id)
);

create table partner_members
(
    id            binary(16)   not null
        primary key,
    created_at    datetime(6)  null,
    updated_at    datetime(6)  null,
    email         varchar(255) null,
    image_url     varchar(500) null,
    name          varchar(255) null,
    name_en       varchar(100) null,
    role          varchar(100) null,
    partner_id    binary(16)   not null,
    display_order int          null,
    constraint FK148aym0l8ydnkillg2277l35a
        foreign key (partner_id) references partners (id)
);

create table plans
(
    id          binary(16)     not null
        primary key,
    created_at  datetime(6)    null,
    updated_at  datetime(6)    null,
    description varchar(300)   null,
    is_active   bit            null,
    name        varchar(100)   null,
    price       decimal(10, 2) null
);

create table refresh_token
(
    id    bigint auto_increment
        primary key,
    email varchar(255) not null,
    token varchar(255) not null,
    constraint UK6t7skxndr9jtm3ckw71g75tfl
        unique (email)
);

create table subscriptions
(
    id         binary(16)                             not null
        primary key,
    created_at datetime(6)                            null,
    updated_at datetime(6)                            null,
    ended_at   datetime(6)                            null,
    started_at datetime(6)                            null,
    status     enum ('ACTIVE', 'CANCELED', 'EXPIRED') null,
    account_id binary(16)                             null,
    plan_id    binary(16)                             null,
    constraint FK2i5t5e392cxe52a84xmkdl0os
        foreign key (account_id) references accounts (id),
    constraint FKb1uf5qnxi6uj95se8ykydntl1
        foreign key (plan_id) references plans (id)
);
