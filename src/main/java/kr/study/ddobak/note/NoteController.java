package kr.study.ddobak.note;

import jakarta.validation.Valid;
import java.util.List;
import kr.study.ddobak.auth.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse create(@LoginUser Long userId, @Valid @RequestBody NoteRequest req) {
        return noteService.create(userId, req);
    }

    @GetMapping
    public List<NoteSummary> list(@LoginUser Long userId) {
        return noteService.list(userId);
    }

    @GetMapping("/{id}")
    public NoteResponse get(@LoginUser Long userId, @PathVariable Long id) {
        return noteService.get(userId, id);
    }

    @PutMapping("/{id}")
    public NoteResponse update(@LoginUser Long userId, @PathVariable Long id, @Valid @RequestBody NoteRequest req) {
        return noteService.update(userId, id, req);
    }

    @PatchMapping("/{id}/folder")
    public NoteSummary moveToFolder(@LoginUser Long userId, @PathVariable Long id, @RequestBody NoteFolderRequest req) {
        return noteService.moveToFolder(userId, id, req.folderId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@LoginUser Long userId, @PathVariable Long id) {
        noteService.delete(userId, id);
    }
}
