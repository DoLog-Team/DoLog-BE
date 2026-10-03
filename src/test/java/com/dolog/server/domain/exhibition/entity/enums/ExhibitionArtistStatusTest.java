package com.dolog.server.domain.exhibition.entity.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExhibitionArtistStatusTest {

    @Test
    @DisplayName("관리자는 JOINED를 제외한 기존 참여 상태의 작가를 직접 추가할 수 있다")
    void checksWhetherArtistCanBeAddedByAdmin() {
        for (ExhibitionArtistStatus status : List.of(
                ExhibitionArtistStatus.PENDING,
                ExhibitionArtistStatus.DENIED,
                ExhibitionArtistStatus.WITHDRAWN,
                ExhibitionArtistStatus.REMOVED
        )) {
            assertTrue(status.canBeAddedByAdmin());
        }

        assertFalse(ExhibitionArtistStatus.JOINED.canBeAddedByAdmin());
    }
}
