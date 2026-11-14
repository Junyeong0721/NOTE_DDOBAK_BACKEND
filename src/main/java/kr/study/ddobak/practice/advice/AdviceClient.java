package kr.study.ddobak.practice.advice;

/** 연습 결과를 보고 고칠 점을 한두 문장으로 알려 주는 AI. 다른 AI로 바꿀 때는 이 인터페이스만 새로 구현한다 */
public interface AdviceClient {

    /** @throws AdviceFailedException 조언을 받지 못했을 때 */
    String advise(AdviceRequest request);
}
