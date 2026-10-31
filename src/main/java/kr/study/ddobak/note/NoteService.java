package kr.study.ddobak.note;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
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

    /**
     * 필기 데이터는 프론트가 만든 JSON이다. 저장 전에 최소 형식만 확인한다:
     * JSON 객체이고, 숫자 version과 배열 pages가 있어야 한다. (획 내용은 프론트가 책임진다)
     */
    private void validateContent(String content) {
        JsonNode root;
        try {
            root = jsonMapper.readTree(content);
        } catch (JacksonException e) {
            throw new InvalidContentException();
        }
        boolean valid = root != null && root.isObject()
                && root.path("version").isNumber()
                && root.path("pages").isArray();
        if (!valid) {
            throw new InvalidContentException();
        }
    }
}
