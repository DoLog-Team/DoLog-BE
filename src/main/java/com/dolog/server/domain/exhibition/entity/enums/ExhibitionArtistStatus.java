package com.dolog.server.domain.exhibition.entity.enums;

public enum ExhibitionArtistStatus {
    PENDING, JOINED, DENIED, WITHDRAWN, REMOVED;

    public boolean canChangeTo(ExhibitionArtistStatus nextStatus) {
        return switch (this) {
            case PENDING ->
                    nextStatus == JOINED
                            || nextStatus == DENIED;

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
