package kr.study.ddobak.note.summary;

/** 오늘 요약 횟수를 다 씀 → 429 */
public class SummaryLimitExceededException extends RuntimeException {
    public SummaryLimitExceededException(int limit) {
        super("오늘 AI 요약을 다 썼어요. (하루 " + limit + "번) 내일 다시 해 주세요.");
    }
}
