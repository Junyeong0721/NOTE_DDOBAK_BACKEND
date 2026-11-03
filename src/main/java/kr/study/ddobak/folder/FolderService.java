package kr.study.ddobak.folder;

import java.util.List;
import kr.study.ddobak.note.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FolderService {

    private final FolderRepository folderRepository;
    private final NoteRepository noteRepository;

    public List<FolderResponse> list() {
        return folderRepository.findAll(Sort.by("createdAt").ascending().and(Sort.by("id"))).stream()
                .map(FolderResponse::from)
                .toList();
    }

    @Transactional
    public FolderResponse create(FolderRequest req) {
        return FolderResponse.from(folderRepository.save(new Folder(req.name().trim(), req.color())));
    }

    @Transactional
    public FolderResponse update(Long id, FolderRequest req) {
        Folder folder = find(id);
        folder.update(req.name().trim(), req.color());
        return FolderResponse.from(folder);
    }

    /** 폴더를 지워도 노트는 지우지 않고 "미분류"(folderId = null)로 옮긴다 */
    @Transactional
    public void delete(Long id) {
        Folder folder = find(id);
        noteRepository.unassignFolder(id);
        folderRepository.delete(folder);
    }

    private Folder find(Long id) {
        return folderRepository.findById(id).orElseThrow(() -> new FolderNotFoundException(id));
    }
}
