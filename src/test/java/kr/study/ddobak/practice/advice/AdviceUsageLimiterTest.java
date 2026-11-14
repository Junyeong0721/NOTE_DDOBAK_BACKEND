package kr.study.ddobak.practice.advice;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AdviceUsageLimiterTest {

    static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-14T03:00:00Z"));
    final Clock clock = new Clock() {
        @Override public ZoneId getZone() { return SEOUL; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    };
    final AdviceUsageLimiter limiter = new AdviceUsageLimiter(2, clock);

    @Test
    void stopsAfterTheDailyLimit_perAccount() {
        assertThat(limiter.tryAcquire(1L)).isTrue();
        assertThat(limiter.tryAcquire(1L)).isTrue();
        assertThat(limiter.tryAcquire(1L)).isFalse();
        assertThat(limiter.tryAcquire(2L)).isTrue(); // 다른 회원은 따로 센다
    }

    @Test
    void release_givesBackAFailedCall() {
        limiter.tryAcquire(1L);
        limiter.tryAcquire(1L);
        limiter.release(1L);
        assertThat(limiter.tryAcquire(1L)).isTrue();
        assertThat(limiter.tryAcquire(1L)).isFalse();
        limiter.release(9L); // 쓴 적 없는 회원을 돌려줘도 문제없음
        assertThat(limiter.tryAcquire(9L)).isTrue();
    }

    @Test
    void resetsAtMidnightInKorea() {
        limiter.tryAcquire(1L);
        limiter.tryAcquire(1L);
        now.set(Instant.parse("2026-11-14T14:59:59Z")); // 한국 23:59:59
        assertThat(limiter.tryAcquire(1L)).isFalse();
        now.set(Instant.parse("2026-11-14T15:00:00Z")); // 한국 다음 날 0시
        assertThat(limiter.tryAcquire(1L)).isTrue();
    }
}
