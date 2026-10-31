package kr.study.ddobak.note;

import java.time.Instant;

public record NoteResponse(Long id, String title, String content, Instant createdAt, Instant updatedAt) {

    static NoteResponse from(Note note) {
        return new NoteResponse(note.getId(), note.getTitle(), note.getContent(),
                note.getCreatedAt(), note.getUpdatedAt());
    }
}
