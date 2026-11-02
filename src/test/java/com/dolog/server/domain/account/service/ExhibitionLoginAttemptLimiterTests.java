package com.dolog.server.domain.account.service;

import com.dolog.server.domain.account.exception.AccountErrorCode;
import com.dolog.server.global.exception.BaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExhibitionLoginAttemptLimiterTests {

    private static final String IP = "1.2.3.4";

    @Test
    @DisplayName("5회 연속 실패하면 10분간 막히고, 10분이 지나면 풀린다.")
    void locksAfterFiveFailuresForTenMinutes() {
        Clock clock = mock(Clock.class);
        Instant start = Instant.parse("2026-11-02T00:00:00Z");
        when(clock.instant()).thenReturn(start);
        var limiter = new ExhibitionLoginAttemptLimiter(clock);

        for (int i = 0; i < 4; i++) {
            limiter.recordFailure(IP);
        }
        limiter.requireNotLocked(IP);
        var locked = assertThrows(BaseException.class, () -> limiter.recordFailure(IP));
        assertEquals(AccountErrorCode.EXHIBITION_LOGIN_LOCKED, locked.getErrorCode());

        when(clock.instant()).thenReturn(start.plusSeconds(9 * 60));
        assertThrows(BaseException.class, () -> limiter.requireNotLocked(IP));
        limiter.requireNotLocked("5.6.7.8");

        when(clock.instant()).thenReturn(start.plusSeconds(10 * 60));
        limiter.requireNotLocked(IP);
        limiter.recordFailure(IP);
    }

    @Test
    @DisplayName("로그인에 성공하면 실패 횟수가 초기화된다.")
    void resetClearsFailures() {
        var limiter = new ExhibitionLoginAttemptLimiter();
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure(IP);
        }
        limiter.reset(IP);
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure(IP);
        }
        limiter.requireNotLocked(IP);
    }
}
