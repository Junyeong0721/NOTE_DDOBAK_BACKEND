package kr.study.ddobak.note.summary;

import java.time.Instant;
import java.util.List;

/** summary: 저장된 요약 줄들, title: 제목 추천(저장 안 함, 없으면 null) */
public record NoteSummaryResponse(List<String> summary, String title, Instant summarizedAt) {
}
