package kr.study.ddobak.note.summary;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** AI 요약을 회원마다 하루(한국 시간) N번으로 막는다. 메모리에만 세므로 서버를 다시 켜면 0부터 */
@Component
public class SummaryUsageLimiter {

    private final int dailyLimit;
    private final Clock clock;
    private final Map<Long, Integer> used = new HashMap<>();
    private LocalDate day;

    @Autowired
    public SummaryUsageLimiter(@Value("${ddobak.summary.daily-limit:20}") int dailyLimit) {
        this(dailyLimit, Clock.system(ZoneId.of("Asia/Seoul")));
    }

    SummaryUsageLimiter(int dailyLimit, Clock clock) {
        this.dailyLimit = dailyLimit;
        this.clock = clock;
    }

    public int dailyLimit() {
        return dailyLimit;
    }

    public synchronized boolean tryAcquire(Long userId) {
        rollDay();
        int count = used.getOrDefault(userId, 0);
        if (count >= dailyLimit) return false;
        used.put(userId, count + 1);
        return true;
    }

    /** 요약을 받지 못했으면 쓴 횟수를 돌려준다 */
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
