package kr.study.ddobak.practice.advice;

/** 조언을 받지 못함 (연결 실패, 한도 초과, 빈 응답 등). 화면에는 "지금은 조언을 할 수 없어요"로만 보인다 */
public class AdviceFailedException extends RuntimeException {
    public AdviceFailedException(String message) {
        super(message);
    }
}
