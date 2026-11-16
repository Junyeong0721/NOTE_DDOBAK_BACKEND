package kr.study.ddobak.note.summary;

import java.util.List;

/** 노트 페이지 그림을 읽고 요약하는 AI. 다른 AI로 바꿀 때는 이 인터페이스만 새로 구현한다 */
public interface SummaryClient {

    /**
     * @param pages 필기가 있는 페이지 그림(PNG), 순서대로
     * @throws SummaryFailedException 요약을 받지 못했을 때
     */
    NoteSummaryResult summarize(String currentTitle, List<byte[]> pages);
}
