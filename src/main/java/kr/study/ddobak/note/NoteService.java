package kr.study.ddobak.note;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteService {

    private final NoteRepository noteRepository;
    private final JsonMapper jsonMapper;

    @Transactional
    public NoteResponse create(NoteRequest req) {
        validateContent(req.content());
        Note saved = noteRepository.save(new Note(req.title(), req.content()));
        return NoteResponse.from(saved);
    }

    public NoteResponse get(Long id) {
        return NoteResponse.from(findNote(id));
    }

    public List<NoteSummary> list() {
        return noteRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt")).stream()
                .map(NoteSummary::from)
                .toList();
    }

    @Transactional
    public NoteResponse update(Long id, NoteRequest req) {
        validateContent(req.content());
        Note note = findNote(id);
        note.update(req.title(), req.content());
        noteRepository.flush(); // updatedAt(@PreUpdate)을 응답에 반영하기 위해 즉시 반영
        return NoteResponse.from(note);
    }

    @Transactional
    public void delete(Long id) {
        noteRepository.delete(findNote(id));
    }

    private Note findNote(Long id) {
        return noteRepository.findById(id).orElseThrow(() -> new NoteNotFoundException(id));
    }

    /** 필기 데이터는 프론트가 만든 JSON이므로, 저장 전에 JSON으로 읽히는지만 확인한다. */
    private void validateContent(String content) {
        try {
            jsonMapper.readTree(content);
        } catch (JacksonException e) {
            throw new InvalidContentException();
        }
    }
}
