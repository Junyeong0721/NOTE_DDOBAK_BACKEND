package kr.study.ddobak.ocr;

import java.util.List;

/** 실제 인식 없이 빈 결과를 돌려준다. API 키가 없을 때(테스트, 키 설정 전) 쓴다. OcrConfig에서 고른다. */
public class FakeOcrClient implements OcrClient {

    @Override
    public OcrResult recognize(byte[] png, int width, int height) {
        return new OcrResult("fake", "", List.of());
    }
}
