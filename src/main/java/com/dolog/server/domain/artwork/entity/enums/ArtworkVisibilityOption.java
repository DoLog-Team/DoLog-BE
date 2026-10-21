package com.dolog.server.domain.artwork.entity.enums;

// 작가가 직접 고르는 작품 공개 상태 (API 입출력용).
// 완료율로 계산되는 읽기 전용 값인 ArtworkVisibility(PUBLIC/PRIVATE/DRAFT, 전시 어드민 작품 관리 목록 필터용)와는 다른 개념이다.
public enum ArtworkVisibilityOption {
    PUBLIC, PRIVATE, EXHIBITION_ONLY
}
