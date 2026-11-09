package kr.study.ddobak.ocr;

/** 외부 글자 인식 서비스 호출이 실패했을 때 (502) */
public class OcrFailedException extends RuntimeException {

    public OcrFailedException(String message) {
        super(message);
    }
}
