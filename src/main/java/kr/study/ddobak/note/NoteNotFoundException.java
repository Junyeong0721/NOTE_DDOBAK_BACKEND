package kr.study.ddobak.note;

public class NoteNotFoundException extends RuntimeException {

    public NoteNotFoundException(Long id) {
        super("노트를 찾을 수 없습니다. id=" + id);
    }
}
