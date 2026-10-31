package kr.study.ddobak.common;

/** 모든 API 오류 응답의 공통 형식: {"message": "..."} */
public record ErrorResponse(String message) {
}
