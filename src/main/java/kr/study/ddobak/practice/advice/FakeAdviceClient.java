package kr.study.ddobak.practice.advice;

/** API 키가 없을 때 쓰는 가짜 조언 (개발·테스트용) */
public class FakeAdviceClient implements AdviceClient {

    static final String ADVICE = "가짜 조언: 세로 획을 곧게 내려 보세요.";

    @Override
    public String advise(AdviceRequest request) {
        return ADVICE;
    }
}
