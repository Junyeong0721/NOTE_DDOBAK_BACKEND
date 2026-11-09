package kr.study.ddobak.ocr;

/** 하루 사용 횟수나 외부 서비스 한도를 넘었을 때 (429) */
public class OcrLimitExceededException extends RuntimeException {

    public OcrLimitExceededException(String message) {
        super(message);
    }
}
