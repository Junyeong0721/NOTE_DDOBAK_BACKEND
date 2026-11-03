package kr.study.ddobak.folder;

public class FolderNotFoundException extends RuntimeException {

    public FolderNotFoundException(Long id) {
        super("폴더를 찾을 수 없습니다. id=" + id);
    }
}
