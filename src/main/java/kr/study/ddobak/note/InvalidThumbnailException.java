package kr.study.ddobak.note;

public class InvalidThumbnailException extends RuntimeException {

    public InvalidThumbnailException() {
        super("썸네일 형식이 올바르지 않습니다.");
    }
}
