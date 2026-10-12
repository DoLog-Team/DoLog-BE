package com.dolog.server.domain.artwork.entity.enums;

// 화면용 공개 여부. DRAFT 는 작성 진행도 100% 면 비공개, 아니면 임시저장
public enum ArtworkVisibility {
    PUBLIC, PRIVATE, DRAFT;

    public static ArtworkVisibility of(ArtworkStatus status, int completionRate) {
        if (status == ArtworkStatus.PUBLISHED) {
            return PUBLIC;
        }
        return completionRate == 100 ? PRIVATE : DRAFT;
    }
}
