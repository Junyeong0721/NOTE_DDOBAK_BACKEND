package kr.study.ddobak.practice;

import java.time.Instant;

/** 최근 연습 목록용 요약 (획은 뺀다). text는 문제들을 줄바꿈으로 이어 붙인 것 */
public record PracticeAttemptSummary(Long id, String stage, String text, String fontId, int totalScore, Instant createdAt) {

    static PracticeAttemptSummary from(PracticeAttempt a) {
        return new PracticeAttemptSummary(a.getId(), a.getStage(), a.getText(), a.getFontId(), a.getTotalScore(), a.getCreatedAt());
    }
}
