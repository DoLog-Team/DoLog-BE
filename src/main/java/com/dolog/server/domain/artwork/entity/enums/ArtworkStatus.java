package com.dolog.server.domain.artwork.entity.enums;

// DB ENUM 의 HIDDEN 은 쓰지 않는다. 숨김은 status 와 독립된 hidden_at 으로 관리한다.
public enum ArtworkStatus {
    DRAFT, PUBLISHED
}
