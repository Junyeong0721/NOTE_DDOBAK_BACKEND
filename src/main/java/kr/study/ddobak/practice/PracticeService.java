package kr.study.ddobak.practice;

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

    static final int MAX_TEXT = 30;
    static final int MAX_STROKES_LENGTH = 2 * 1024 * 1024;

    private final PracticeAttemptRepository repository;
    private final JsonMapper jsonMapper;

    /** 기록 저장. AI 조언은 이 뒤에 따로 붙인다(조언이 실패해도 기록은 남게) */
    @Transactional
    public PracticeAttemptResponse save(Long userId, PracticeAttemptRequest req) {
        String text = req.text().strip();
        int length = text.codePointCount(0, text.length());
        if (length > MAX_TEXT) {
            throw new InvalidPracticeException("연습할 글은 " + MAX_TEXT + "자 이하로 입력해 주세요.");
        }
        if (!req.cells().isArray() || req.cells().size() != length) {
            throw new InvalidPracticeException("쓴 글씨의 칸 수가 글자 수와 다릅니다.");
        }
        if (!req.charScores().isArray() || req.charScores().size() != length) {
            throw new InvalidPracticeException("글자별 점수의 수가 글자 수와 다릅니다.");
        }
        String strokes = jsonMapper.writeValueAsString(req.cells());
        if (strokes.length() > MAX_STROKES_LENGTH) {
            throw new InvalidPracticeException("쓴 글씨가 너무 많습니다. 칸을 지우고 다시 써 주세요.");
        }
        String setId = req.setId() == null || req.setId().isBlank() ? null : req.setId().strip();
        PracticeAttempt saved = repository.save(PracticeAttempt.builder()
                .userId(userId)
                .setId(setId)
                .text(text)
                .fontId(req.fontId().strip())
                .guide(req.guide())
                .strokes(strokes)
                .charScores(jsonMapper.writeValueAsString(req.charScores()))
                .totalScore(req.totalScore())
                .shapeScore(req.shapeScore())
                .positionScore(req.positionScore())
                .tiltScore(req.tiltScore())
                .build());
        return PracticeAttemptResponse.of(saved, req.cells(), req.charScores());
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
        JsonNode cells = jsonMapper.readTree(a.getStrokes());
        JsonNode scores = jsonMapper.readTree(a.getCharScores());
        return PracticeAttemptResponse.of(a, cells, scores);
    }
}
