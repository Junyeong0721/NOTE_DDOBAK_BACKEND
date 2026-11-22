package kr.study.ddobak.community;

/** 공개하기·정보 수정. refreshContent: 필기 복사본도 지금 노트로 바꿀지 ([공개본 업데이트], 처음 공개는 항상) */
public record PublishRequest(String title, String subject, String intro, boolean refreshContent) {
}
