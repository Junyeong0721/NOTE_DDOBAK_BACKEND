package kr.study.ddobak.note;

import java.time.Instant;

/** 노트 목록용. 필기 데이터(content)는 크기 때문에 목록에서 뺀다. */
public record NoteSummary(Long id, String title, Instant updatedAt) {

    static NoteSummary from(Note note) {
        return new NoteSummary(note.getId(), note.getTitle(), note.getUpdatedAt());
    }
}
