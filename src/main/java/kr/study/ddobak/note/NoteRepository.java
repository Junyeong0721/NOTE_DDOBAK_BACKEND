package kr.study.ddobak.note;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findAllByUserId(Long userId, Sort sort);

    /** 다른 회원의 노트는 없는 것처럼 (404) */
    Optional<Note> findByIdAndUserId(Long id, Long userId);

    /** 폴더를 지울 때 그 폴더의 노트를 미분류로 옮긴다 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Note n set n.folderId = null where n.folderId = :folderId")
    int unassignFolder(@Param("folderId") Long folderId);

    /**
     * AI 요약 저장. 엔티티를 고치면 @PreUpdate가 updatedAt을 바꿔 "요약 뒤에 노트가 바뀌었다"로 보이므로,
     * 수정 시각을 건드리지 않게 쿼리로 바로 바꾼다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Note n set n.summary = :summary, n.summarizedAt = :at where n.id = :id and n.userId = :userId")
    int saveSummary(@Param("id") Long id, @Param("userId") Long userId, @Param("summary") String summary, @Param("at") Instant at);
}
