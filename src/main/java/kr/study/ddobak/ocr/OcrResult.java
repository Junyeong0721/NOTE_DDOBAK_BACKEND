package kr.study.ddobak.ocr;

import java.util.List;

/** engine: 인식에 쓴 엔진 이름, text: 줄 전체 글자, chars: 글자별 위치(줄 이미지 픽셀 좌표)와 신뢰도 */
public record OcrResult(String engine, String text, List<OcrChar> chars) {
}
