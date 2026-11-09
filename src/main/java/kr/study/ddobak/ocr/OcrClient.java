package kr.study.ddobak.ocr;

/** 줄 이미지(PNG)에서 글자를 인식한다. 구현: GoogleVisionOcrClient(실제), FakeOcrClient(키 없을 때). */
public interface OcrClient {

    OcrResult recognize(byte[] png, int width, int height);
}
