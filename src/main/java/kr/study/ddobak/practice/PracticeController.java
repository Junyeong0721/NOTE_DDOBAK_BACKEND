package kr.study.ddobak.practice;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import kr.study.ddobak.auth.LoginUser;
import kr.study.ddobak.practice.advice.AdviceClient;
import kr.study.ddobak.practice.advice.AdviceRequest;
import kr.study.ddobak.practice.advice.AdviceUsageLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@Slf4j
@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
public class PracticeController {

    static final String IMAGE_PREFIX = "data:image/png;base64,";
    static final int MAX_IMAGE_BYTES = 1024 * 1024;
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    private final PracticeService practiceService;
    private final AdviceClient adviceClient;
    private final AdviceUsageLimiter adviceLimiter;

    /**
     * 기록을 먼저 저장하고, 그다음 AI 조언을 붙인다.
     * 조언을 받지 못해도(한도·오류·시간 초과) 기록은 남고 adviceStatus만 UNAVAILABLE이 된다.
     */
    @PostMapping("/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public PracticeAttemptResponse create(@LoginUser Long userId, @Valid @RequestBody PracticeAttemptRequest req) {
        byte[] png = decodeImage(req.image());
        PracticeAttemptResponse saved = practiceService.save(userId, req);
        if (png == null || !adviceLimiter.tryAcquire(userId)) {
            return saved;
        }
        try {
            String label = req.fontLabel() == null || req.fontLabel().isBlank() ? saved.fontId() : req.fontLabel().strip();
            String advice = adviceClient.advise(
                    new AdviceRequest(label, saved.text(), saved.guide(), charScores(saved.text(), req.charScores()), png));
            return practiceService.attachAdvice(userId, saved.id(), advice);
        } catch (RuntimeException e) {
            adviceLimiter.release(userId); // 받지 못한 조언은 오늘 횟수에서 빼지 않는다
            log.warn("연습 조언 실패 (기록 {}): {}", saved.id(), e.getMessage());
            return saved;
        }
    }

    @GetMapping("/attempts")
    public List<PracticeAttemptSummary> recent(@LoginUser Long userId) {
        return practiceService.recent(userId);
    }

    @GetMapping("/attempts/{id}")
    public PracticeAttemptResponse get(@LoginUser Long userId, @PathVariable Long id) {
        return practiceService.get(userId, id);
    }

    @GetMapping("/best")
    public List<PracticeBest> best(@LoginUser Long userId) {
        return practiceService.best(userId);
    }

    /** 조언용 그림. 없으면 null(조언 없이 저장), 있는데 PNG가 아니거나 너무 크면 400 */
    private static byte[] decodeImage(String image) {
        if (image == null || image.isBlank()) return null;
        if (!image.startsWith(IMAGE_PREFIX)) {
            throw new InvalidPracticeException("그림 형식이 올바르지 않습니다.");
        }
        byte[] png;
        try {
            png = Base64.getDecoder().decode(image.substring(IMAGE_PREFIX.length()));
        } catch (IllegalArgumentException e) {
            throw new InvalidPracticeException("그림 형식이 올바르지 않습니다.");
        }
        if (png.length > MAX_IMAGE_BYTES) {
            throw new InvalidPracticeException("그림이 너무 큽니다.");
        }
        if (png.length < PNG_SIGNATURE.length
                || !Arrays.equals(Arrays.copyOf(png, PNG_SIGNATURE.length), PNG_SIGNATURE)) {
            throw new InvalidPracticeException("그림 형식이 올바르지 않습니다.");
        }
        return png;
    }

    /** 글자와 점수를 짝지어 AI에게 줄 목록으로. 띄어쓰기(null)와 채점하지 않은 글자(skipped)는 뺀다 */
    private static List<AdviceRequest.CharScore> charScores(String text, JsonNode scores) {
        int[] chars = text.codePoints().toArray();
        List<AdviceRequest.CharScore> list = new ArrayList<>();
        for (int i = 0; i < chars.length && i < scores.size(); i++) {
            JsonNode s = scores.get(i);
            if (s == null || !s.isObject() || s.path("skipped").asBoolean(false)) continue;
            list.add(new AdviceRequest.CharScore(
                    new String(Character.toChars(chars[i])),
                    round(s.path("total")), round(s.path("shape")), round(s.path("position")), round(s.path("tilt")),
                    s.path("empty").asBoolean(false)));
        }
        return list;
    }

    private static int round(JsonNode n) {
        return (int) Math.round(n.asDouble(0));
    }
}
