package kr.study.ddobak.practice;

import java.time.Instant;

/** 최근 연습 목록용 요약 (획은 뺀다) */
public record PracticeAttemptSummary(Long id, String setId, String text, String fontId, int totalScore, Instant createdAt) {

    static PracticeAttemptSummary from(PracticeAttempt a) {
        return new PracticeAttemptSummary(a.getId(), a.getSetId(), a.getText(), a.getFontId(), a.getTotalScore(), a.getCreatedAt());
    }
}
