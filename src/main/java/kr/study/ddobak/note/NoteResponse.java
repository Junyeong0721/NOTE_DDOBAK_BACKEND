package kr.study.ddobak.note;

import java.time.Instant;

/** summary: AI 요약(줄바꿈으로 이어 붙임, 없으면 null), summarizedAt: 요약한 시각 */
public record NoteResponse(Long id, String title, String content, Long folderId, String thumbnail,
                           String summary, Instant summarizedAt, Instant createdAt, Instant updatedAt) {

    static NoteResponse from(Note note) {
        return new NoteResponse(note.getId(), note.getTitle(), note.getContent(), note.getFolderId(),
                note.getThumbnail(), note.getSummary(), note.getSummarizedAt(), note.getCreatedAt(), note.getUpdatedAt());
    }
}
