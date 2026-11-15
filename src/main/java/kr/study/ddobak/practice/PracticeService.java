package kr.study.ddobak.practice;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PracticeService {

    static final int MAX_ITEMS = 5;
    static final int MAX_ITEM_TEXT = 20;
    static final int MAX_STROKES_LENGTH = 2 * 1024 * 1024;

    private final PracticeAttemptRepository repository;
    private final JsonMapper jsonMapper;

    /** 한 판 저장. AI 조언은 이 뒤에 따로 붙인다(조언이 실패해도 기록은 남게) */
    @Transactional
    public PracticeAttemptResponse save(Long userId, PracticeAttemptRequest req) {
        PracticeStage stage = PracticeStage.of(req.stage().strip())
                .orElseThrow(() -> new InvalidPracticeException("알 수 없는 연습 단계입니다."));
        JsonNode items = req.items();
        if (!items.isArray() || items.isEmpty() || items.size() > MAX_ITEMS) {
            throw new InvalidPracticeException("한 판의 문제는 1~" + MAX_ITEMS + "개여야 합니다.");
        }
        List<String> texts = new ArrayList<>();
        List<JsonNode> scores = new ArrayList<>();
        for (JsonNode item : items) {
            String text = item.path("text").asString("").strip();
            int length = text.codePointCount(0, text.length());
            if (length == 0 || length > MAX_ITEM_TEXT) {
                throw new InvalidPracticeException("문제 글은 1~" + MAX_ITEM_TEXT + "자여야 합니다.");
            }
            if (!item.path("cells").isArray() || item.path("cells").size() != length) {
                throw new InvalidPracticeException("쓴 글씨의 칸 수가 글자 수와 다릅니다.");
            }
            if (!item.path("charScores").isArray() || item.path("charScores").size() != length) {
                throw new InvalidPracticeException("글자별 점수의 수가 글자 수와 다릅니다.");
            }
            if (!item.path("score").isObject()) {
                throw new InvalidPracticeException("문제 점수가 없습니다.");
            }
            texts.add(text);
            scores.add(item.path("score"));
        }
        String strokes = jsonMapper.writeValueAsString(items);
        if (strokes.length() > MAX_STROKES_LENGTH) {
            throw new InvalidPracticeException("쓴 글씨가 너무 많습니다. 칸을 지우고 다시 써 주세요.");
        }
        PracticeAttempt saved = repository.save(PracticeAttempt.builder()
                .userId(userId)
                .stage(stage.id())
                .text(String.join("\n", texts))
                .fontId(req.fontId().strip())
                .guide(req.guide())
                .strokes(strokes)
                .charScores(jsonMapper.writeValueAsString(scores))
                .totalScore(req.totalScore())
                .shapeScore(req.shapeScore())
                .positionScore(req.positionScore())
                .tiltScore(req.tiltScore())
                .build());
        return PracticeAttemptResponse.of(saved, items);
    }

    @Transactional
    public PracticeAttemptResponse attachAdvice(Long userId, Long id, String advice) {
        PracticeAttempt attempt = find(userId, id);
        attempt.attachAdvice(advice);
        return toResponse(attempt);
    }

    public PracticeAttemptResponse get(Long userId, Long id) {
        return toResponse(find(userId, id));
    }

    public List<PracticeAttemptSummary> recent(Long userId) {
        return repository.findTop20ByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
                .map(PracticeAttemptSummary::from)
                .toList();
    }

    public List<PracticeBest> best(Long userId) {
        return repository.findBestByUserId(userId);
    }

    private PracticeAttempt find(Long userId, Long id) {
        return repository.findByIdAndUserId(id, userId).orElseThrow(PracticeNotFoundException::new);
    }

    private PracticeAttemptResponse toResponse(PracticeAttempt a) {
        return PracticeAttemptResponse.of(a, jsonMapper.readTree(a.getStrokes()));
    }
}
