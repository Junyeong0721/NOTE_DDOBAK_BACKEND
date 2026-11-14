package kr.study.ddobak.practice;

import java.time.Instant;
import tools.jackson.databind.JsonNode;

/** 연습 기록 하나(획 포함). adviceStatus: OK = 조언 있음, UNAVAILABLE = 조언을 받지 못함 */
public record PracticeAttemptResponse(
        Long id,
        String setId,
        String text,
        String fontId,
        boolean guide,
        JsonNode cells,
        JsonNode charScores,
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

    static PracticeAttemptResponse of(PracticeAttempt a, JsonNode cells, JsonNode charScores) {
        return new PracticeAttemptResponse(a.getId(), a.getSetId(), a.getText(), a.getFontId(), a.isGuide(), cells,
                charScores, a.getTotalScore(), a.getShapeScore(), a.getPositionScore(), a.getTiltScore(),
                a.getAdvice(), a.getAdvice() != null ? ADVICE_OK : ADVICE_UNAVAILABLE, a.getCreatedAt());
    }
}
