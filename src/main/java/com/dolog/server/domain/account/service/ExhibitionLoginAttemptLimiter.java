package com.dolog.server.domain.account.service;

import com.dolog.server.domain.account.exception.AccountErrorCode;
import com.dolog.server.global.exception.BaseException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 전시 코드 로그인을 IP별로 5회 연속 실패하면 10분간 막는다.
// ponytail: 서버 메모리 기준이라 재시작하면 풀리고 인스턴스끼리 공유되지 않음. 다중 인스턴스가 되면 Redis로 옮긴다.
@Component
public class ExhibitionLoginAttemptLimiter {

    static final int MAX_FAILURES = 5;
    static final Duration LOCK_DURATION = Duration.ofMinutes(10);

    private record Attempt(int failures, Instant expiresAt) {
    }

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public ExhibitionLoginAttemptLimiter() {
        this(Clock.systemUTC());
    }

    ExhibitionLoginAttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    public void requireNotLocked(String clientKey) {
        Attempt attempt = attempts.get(clientKey);
        if (attempt != null && attempt.failures() >= MAX_FAILURES && attempt.expiresAt().isAfter(clock.instant())) {
            throw new BaseException(AccountErrorCode.EXHIBITION_LOGIN_LOCKED);
        }
    }

    // 실패할 때마다 만료 시각을 10분 뒤로 미루고, 5번째 실패면 제한 오류를 던진다.
    public void recordFailure(String clientKey) {
        Instant now = clock.instant();
        attempts.values().removeIf(attempt -> !attempt.expiresAt().isAfter(now));
        Attempt updated = attempts.merge(clientKey, new Attempt(1, now.plus(LOCK_DURATION)),
                (old, first) -> new Attempt(old.failures() + 1, first.expiresAt()));
        if (updated.failures() >= MAX_FAILURES) {
            throw new BaseException(AccountErrorCode.EXHIBITION_LOGIN_LOCKED);
        }
    }

    public void reset(String clientKey) {
        attempts.remove(clientKey);
    }
}
