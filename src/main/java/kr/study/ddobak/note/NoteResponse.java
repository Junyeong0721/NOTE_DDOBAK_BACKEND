package kr.study.ddobak.note;

import java.time.Instant;

public record NoteResponse(Long id, String title, String content, Long folderId, String thumbnail,
                           Instant createdAt, Instant updatedAt) {

    static NoteResponse from(Note note) {
        return new NoteResponse(note.getId(), note.getTitle(), note.getContent(), note.getFolderId(),
                note.getThumbnail(), note.getCreatedAt(), note.getUpdatedAt());
    }
}
