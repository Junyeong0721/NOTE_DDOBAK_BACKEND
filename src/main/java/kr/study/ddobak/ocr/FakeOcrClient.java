package kr.study.ddobak.ocr;

import java.util.List;
import org.springframework.stereotype.Component;

/** 실제 인식 없이 빈 결과를 돌려준다. 프론트의 "줄 끝 → 인식 요청" 흐름을 먼저 만들기 위한 자리 표시. */
@Component
public class FakeOcrClient implements OcrClient {

    @Override
    public OcrResult recognize(byte[] png, int width, int height) {
        return new OcrResult("fake", "", List.of());
    }
}
