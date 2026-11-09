package kr.study.ddobak.ocr;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 글자 인식 호출을 하루(한국 시간) N번으로 막는다. Google Vision 무료 한도(월 1,000번)를 넘지 않게 하는 비용 상한.
 * 서버가 터널로 외부에 열려 있어서 누가 반복 호출해도 요금이 나가지 않게 서버에서 센다.
 * 횟수는 메모리에만 두므로 서버를 다시 켜면 0부터 센다.
 */
@Component
public class OcrUsageLimiter {

    private final int dailyLimit;
    private final Clock clock;
    private LocalDate day;
    private int used;

    @Autowired
    public OcrUsageLimiter(@Value("${ddobak.ocr.daily-limit:30}") int dailyLimit) {
        this(dailyLimit, Clock.system(ZoneId.of("Asia/Seoul")));
    }

    OcrUsageLimiter(int dailyLimit, Clock clock) {
        this.dailyLimit = dailyLimit;
        this.clock = clock;
    }

    public synchronized void acquire() {
        LocalDate today = LocalDate.now(clock);
        if (!today.equals(day)) {
            day = today;
            used = 0;
        }
        if (used >= dailyLimit) {
            throw new OcrLimitExceededException(
                    "오늘 글자 인식 횟수(" + dailyLimit + "번)를 다 썼습니다. 내일 다시 시도해 주세요.");
        }
        used++;
    }
}
