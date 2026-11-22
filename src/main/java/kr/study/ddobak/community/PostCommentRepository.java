package kr.study.ddobak.community;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {

    List<PostComment> findTop200ByPostIdOrderByCreatedAtAscIdAsc(Long postId);
}
