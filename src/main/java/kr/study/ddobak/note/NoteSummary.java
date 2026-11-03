package kr.study.ddobak.note;

import java.time.Instant;

/** 노트 목록용. 필기 데이터(content)는 크기 때문에 빼고, 미리보기(thumbnail)를 넣는다. */
public record NoteSummary(Long id, String title, Long folderId, String thumbnail, Instant updatedAt) {

    static NoteSummary from(Note note) {
        return new NoteSummary(note.getId(), note.getTitle(), note.getFolderId(), note.getThumbnail(),
                note.getUpdatedAt());
    }
}
