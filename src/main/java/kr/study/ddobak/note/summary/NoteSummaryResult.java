package kr.study.ddobak.note.summary;

import java.util.List;

/** AI가 만든 노트 요약: 핵심 요약 1~5줄 + 제목 추천(못 받았으면 null) */
public record NoteSummaryResult(List<String> lines, String title) {
}
