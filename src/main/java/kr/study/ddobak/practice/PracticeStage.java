package kr.study.ddobak.practice;

import java.util.Arrays;
import java.util.Optional;

/** 연습 단계. 화면에서 단계를 고르면 그 단계의 문제 5개가 랜덤으로 나온다 */
public enum PracticeStage {
    SYLLABLE("syllable", "글자"),
    WORD("word", "단어"),
    SENTENCE("sentence", "문장");

    private final String id;
    private final String label;

    PracticeStage(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public static Optional<PracticeStage> of(String id) {
        return Arrays.stream(values()).filter(s -> s.id.equals(id)).findFirst();
    }
}
