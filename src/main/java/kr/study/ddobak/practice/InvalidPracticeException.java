package kr.study.ddobak.practice;

/** 연습 기록 요청 값이 올바르지 않을 때 (400) */
public class InvalidPracticeException extends RuntimeException {
    public InvalidPracticeException(String message) {
        super(message);
    }
}
