package kr.study.ddobak.practice;

public class PracticeNotFoundException extends RuntimeException {
    public PracticeNotFoundException() {
        super("연습 기록을 찾을 수 없습니다.");
    }
}
