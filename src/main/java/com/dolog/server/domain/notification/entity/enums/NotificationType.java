package com.dolog.server.domain.notification.entity.enums;

// 알림 종류. 대상은 주석의 역할 기준
public enum NotificationType {

    // 작가 알림
    ARTIST_JOIN_APPROVED,       // 참여 승인
    ARTIST_JOIN_REJECTED,       // 참여 거절
    ARTIST_REMOVED,             // 참여 제외
    ZONE_CREATED,               // 작품 그룹 추가
    ZONE_DELETED,               // 작품 그룹 삭제
    ARTWORK_HIDDEN,             // 작품 숨김 처리 (관리자 조치)

    // 전시 어드민 알림
    ARTIST_JOIN_REQUESTED,      // 작가 참여 신청
    ARTIST_WITHDRAWN,           // 소속 작가 참여 종료
    ARTWORK_CANCELLED,          // 소속 작가 작품 출품 취소
    ARTIST_ACCOUNT_DELETED,     // 소속 작가 계정 탈퇴
    PLAN_EXPIRING_SOON,         // 플랜 만료 임박
    PLAN_LIMIT_EXCEEDED         // 플랜 한도 초과
}
