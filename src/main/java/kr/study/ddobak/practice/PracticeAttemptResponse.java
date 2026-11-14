package kr.study.ddobak.practice;

import java.time.Instant;
import tools.jackson.databind.JsonNode;

/** 연습 한 판(획 포함). adviceStatus: OK = 조언 있음, UNAVAILABLE = 조언을 받지 못함 */
public record PracticeAttemptResponse(
        Long id,
        String stage,
        String text,
        String fontId,
        boolean guide,
        JsonNode items,
        int totalScore,
        int shapeScore,
        int positionScore,
        int tiltScore,
        String advice,
        String adviceStatus,
        Instant createdAt
) {
    static final String ADVICE_OK = "OK";
    static final String ADVICE_UNAVAILABLE = "UNAVAILABLE";

    static PracticeAttemptResponse of(PracticeAttempt a, JsonNode items) {
        return new PracticeAttemptResponse(a.getId(), a.getStage(), a.getText(), a.getFontId(), a.isGuide(), items,
                a.getTotalScore(), a.getShapeScore(), a.getPositionScore(), a.getTiltScore(),
                a.getAdvice(), a.getAdvice() != null ? ADVICE_OK : ADVICE_UNAVAILABLE, a.getCreatedAt());
    }
}
