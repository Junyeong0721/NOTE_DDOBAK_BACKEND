package kr.study.ddobak.community;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    Optional<CommunityPost> findByNoteId(Long noteId);

    /** 좋아요 수를 실제 좋아요 줄 수로 다시 센다 (두 번 눌러도 정확) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CommunityPost p set p.likeCount = (select count(l) from PostLike l where l.id.postId = :id) where p.id = :id")
    void recountLikes(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CommunityPost p set p.commentCount = (select count(c) from PostComment c where c.postId = :id) where p.id = :id")
    void recountComments(@Param("id") Long id);

    /** 신고 수를 다시 세고, hideAt 이상이면 숨긴다 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CommunityPost p set
                p.reportCount = (select count(r) from PostReport r where r.id.postId = :id),
                p.hidden = case when (select count(r) from PostReport r where r.id.postId = :id) >= :hideAt then true else false end
            where p.id = :id""")
    void recountReports(@Param("id") Long id, @Param("hideAt") long hideAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update CommunityPost p set p.copyCount = p.copyCount + 1 where p.id = :id")
    void addCopy(@Param("id") Long id);
}
