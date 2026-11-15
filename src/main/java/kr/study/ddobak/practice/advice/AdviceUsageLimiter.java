package kr.study.ddobak.practice.advice;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AI 조언을 회원마다 하루(한국 시간) N번으로 막는다. Gemini 무료 등급의 하루 한도에 닿기 전에 멈추려는 것.
 * 횟수는 메모리에만 두므로 서버를 다시 켜면 0부터 센다.
 */
@Component
public class AdviceUsageLimiter {

    private final int dailyLimit;
    private final Clock clock;
    private final Map<Long, Integer> used = new HashMap<>();
    private LocalDate day;

    @Autowired
    public AdviceUsageLimiter(@Value("${ddobak.advice.daily-limit:50}") int dailyLimit) {
        this(dailyLimit, Clock.system(ZoneId.of("Asia/Seoul")));
    }

    AdviceUsageLimiter(int dailyLimit, Clock clock) {
        this.dailyLimit = dailyLimit;
        this.clock = clock;
    }

    /** 오늘 횟수가 남았으면 한 번 쓰고 true */
    public synchronized boolean tryAcquire(Long userId) {
        rollDay();
        int count = used.getOrDefault(userId, 0);
        if (count >= dailyLimit) return false;
        used.put(userId, count + 1);
        return true;
    }

    /** 조언을 받지 못했으면 쓴 횟수를 돌려준다 */
    public synchronized void release(Long userId) {
        rollDay();
        used.computeIfPresent(userId, (id, count) -> count > 1 ? count - 1 : null);
    }

    private void rollDay() {
        LocalDate today = LocalDate.now(clock);
        if (!today.equals(day)) {
            day = today;
            used.clear();
        }
    }
}
