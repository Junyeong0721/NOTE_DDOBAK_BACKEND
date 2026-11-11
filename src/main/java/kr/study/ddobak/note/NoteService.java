package kr.study.ddobak.note;

import java.util.List;
import kr.study.ddobak.folder.FolderNotFoundException;
import kr.study.ddobak.folder.FolderRepository;
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

    static final String THUMBNAIL_PREFIX = "data:image/png;base64,";
    static final int MAX_THUMBNAIL_LENGTH = 300_000;

    private final NoteRepository noteRepository;
    private final FolderRepository folderRepository;
    private final JsonMapper jsonMapper;

    @Transactional
    public NoteResponse create(Long userId, NoteRequest req) {
        validateContent(req.content());
        validateThumbnail(req.thumbnail());
        validateFolder(userId, req.folderId());
        Note note = new Note(userId, req.title(), req.content(), req.folderId());
        note.changeThumbnail(req.thumbnail());
        return NoteResponse.from(noteRepository.save(note));
    }

    public NoteResponse get(Long userId, Long id) {
        return NoteResponse.from(findNote(userId, id));
    }

    public List<NoteSummary> list(Long userId) {
        return noteRepository.findAllByUserId(userId, Sort.by(Sort.Direction.DESC, "updatedAt")).stream()
                .map(NoteSummary::from)
                .toList();
    }

    @Transactional
    public NoteResponse update(Long userId, Long id, NoteRequest req) {
        validateContent(req.content());
        validateThumbnail(req.thumbnail());
        Note note = findNote(userId, id);
        note.update(req.title(), req.content());
        if (req.thumbnail() != null) {
            note.changeThumbnail(req.thumbnail());
        }
        noteRepository.flush(); // updatedAt(@PreUpdate)을 응답에 반영하기 위해 즉시 반영
        return NoteResponse.from(note);
    }

    @Transactional
    public NoteSummary moveToFolder(Long userId, Long id, Long folderId) {
        Note note = findNote(userId, id);
        validateFolder(userId, folderId);
        note.moveToFolder(folderId);
        noteRepository.flush();
        return NoteSummary.from(note);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        noteRepository.delete(findNote(userId, id));
    }

    private Note findNote(Long userId, Long id) {
        return noteRepository.findByIdAndUserId(id, userId).orElseThrow(() -> new NoteNotFoundException(id));
    }

    private void validateFolder(Long userId, Long folderId) {
        if (folderId != null && !folderRepository.existsByIdAndUserId(folderId, userId)) {
            throw new FolderNotFoundException(folderId);
        }
    }

    private void validateThumbnail(String thumbnail) {
        if (thumbnail != null
                && (!thumbnail.startsWith(THUMBNAIL_PREFIX) || thumbnail.length() > MAX_THUMBNAIL_LENGTH)) {
            throw new InvalidThumbnailException();
        }
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
