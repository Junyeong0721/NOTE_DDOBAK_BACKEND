package kr.study.ddobak.note.summary;

import java.util.List;

/** pages: 필기가 있는 페이지를 그린 PNG data URL들 (1~10장) */
public record NoteSummaryRequest(List<String> pages) {
}
