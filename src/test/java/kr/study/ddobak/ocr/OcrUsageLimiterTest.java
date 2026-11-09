package kr.study.ddobak.ocr;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class OcrUsageLimiterTest {

    static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    /** 시계를 바꿀 수 있게 한 테스트용 Clock */
    static class MutableClock extends Clock {
        Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        @Override public ZoneId getZone() { return SEOUL; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test
    void acquire_overDailyLimit_throws() {
        OcrUsageLimiter limiter = new OcrUsageLimiter(2, Clock.fixed(Instant.parse("2026-11-09T03:00:00Z"), SEOUL));

        limiter.acquire();
        limiter.acquire();

        assertThatThrownBy(limiter::acquire)
                .isInstanceOf(OcrLimitExceededException.class)
                .hasMessage("오늘 글자 인식 횟수(2번)를 다 썼습니다. 내일 다시 시도해 주세요.");
    }

    @Test
    void acquire_resetsAtKoreanMidnight() {
        // 한국 시간 11/9 23:59 → 11/10 00:01 (UTC로는 같은 날 14:59 → 15:01)
        MutableClock clock = new MutableClock(Instant.parse("2026-11-09T14:59:00Z"));
        OcrUsageLimiter limiter = new OcrUsageLimiter(1, clock);
        limiter.acquire();
        assertThatThrownBy(limiter::acquire).isInstanceOf(OcrLimitExceededException.class);

        clock.now = Instant.parse("2026-11-09T15:01:00Z").atOffset(ZoneOffset.UTC).toInstant();

        assertThatCode(limiter::acquire).doesNotThrowAnyException();
    }
}
