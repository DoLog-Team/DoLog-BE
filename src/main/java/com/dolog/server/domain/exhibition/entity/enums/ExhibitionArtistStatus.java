package com.dolog.server.domain.exhibition.entity.enums;

public enum ExhibitionArtistStatus {
    PENDING, JOINED, DENIED, WITHDRAWN, REMOVED;

    public boolean canChangeTo(ExhibitionArtistStatus nextStatus) {
        return switch (this) {
            case PENDING ->
                    nextStatus == JOINED
                            || nextStatus == DENIED
                            // 작가 본인이 수락 대기중인 참여 신청을 취소하는 경우. 관리자 거절(DENIED)과는 의미가 다르다.
                            || nextStatus == WITHDRAWN;

            case JOINED ->
                    nextStatus == WITHDRAWN
                            || nextStatus == REMOVED;

            case DENIED, WITHDRAWN, REMOVED ->
                    nextStatus == PENDING;
        };
    }

    public boolean canBeAddedByAdmin() {
        return switch (this) {
            case PENDING, DENIED, WITHDRAWN, REMOVED -> true;
            case JOINED -> false;
        };
    }

    public boolean blocksReapplication() {
        return this == PENDING || this == JOINED;
    }
}
