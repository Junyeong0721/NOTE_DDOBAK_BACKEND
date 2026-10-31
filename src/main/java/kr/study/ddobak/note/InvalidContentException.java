package kr.study.ddobak.note;

public class InvalidContentException extends RuntimeException {

    public InvalidContentException() {
        super("필기 데이터 형식이 올바르지 않습니다.");
    }
}
