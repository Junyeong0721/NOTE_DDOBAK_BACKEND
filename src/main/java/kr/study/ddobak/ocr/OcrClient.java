package kr.study.ddobak.ocr;

/** 줄 이미지(PNG)에서 글자를 인식한다. 2주차는 가짜 구현, 3주차에 Google Cloud Vision 구현으로 교체한다. */
public interface OcrClient {

    OcrResult recognize(byte[] png, int width, int height);
}
