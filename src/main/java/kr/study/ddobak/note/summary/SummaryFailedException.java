package kr.study.ddobak.note.summary;

/** 요약을 받지 못함 (연결 실패, 빈 응답 등) → 503 "지금은 요약을 만들 수 없어요" */
public class SummaryFailedException extends RuntimeException {
    public SummaryFailedException(String message) {
        super(message);
    }
}
