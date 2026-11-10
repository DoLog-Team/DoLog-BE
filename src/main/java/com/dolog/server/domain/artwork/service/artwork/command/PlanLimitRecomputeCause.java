package com.dolog.server.domain.artwork.service.artwork.command;

// ArtworkPlanLimitService.recompute()를 호출한 이유
public enum PlanLimitRecomputeCause {
    // 구독/플랜 변경(업그레이드·다운그레이드·취소 등)으로 호출된 경우
    PLAN_CHANGE,
    // 그 외 — 출품, 작품 숨김/숨김 해제, 구역 변경 등 개별 작품 관리로 호출된 경우
    ARTWORK_CHANGE
}
