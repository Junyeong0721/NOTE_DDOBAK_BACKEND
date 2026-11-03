package kr.study.ddobak.note;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoteRepository extends JpaRepository<Note, Long> {

    /** 폴더를 지울 때 그 폴더의 노트를 미분류로 옮긴다 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Note n set n.folderId = null where n.folderId = :folderId")
    int unassignFolder(@Param("folderId") Long folderId);
}
