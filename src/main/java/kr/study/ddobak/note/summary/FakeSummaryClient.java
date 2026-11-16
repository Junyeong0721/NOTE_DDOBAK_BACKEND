package kr.study.ddobak.note.summary;

import java.util.List;

/** API 키가 없을 때 쓰는 가짜 요약 (개발·테스트용) */
public class FakeSummaryClient implements SummaryClient {

    @Override
    public NoteSummaryResult summarize(String currentTitle, List<byte[]> pages) {
        return new NoteSummaryResult(List.of(
                "가짜 요약: 페이지 " + pages.size() + "장을 읽었어요.",
                "Gemini 키를 넣으면 실제 필기 내용으로 요약해요."), "가짜 제목 추천");
    }
}
