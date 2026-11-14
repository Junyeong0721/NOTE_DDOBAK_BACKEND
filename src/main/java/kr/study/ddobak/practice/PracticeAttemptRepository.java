package kr.study.ddobak.practice;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PracticeAttemptRepository extends JpaRepository<PracticeAttempt, Long> {

    /** 다른 회원의 기록은 없는 것처럼 (404) */
    Optional<PracticeAttempt> findByIdAndUserId(Long id, Long userId);

    List<PracticeAttempt> findTop20ByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    /** 단계마다 최고 점수·판 수·마지막 연습 시각 */
    @Query("""
            select new kr.study.ddobak.practice.PracticeBest(a.stage, max(a.totalScore), count(a), max(a.createdAt))
            from PracticeAttempt a where a.userId = :userId group by a.stage
            """)
    List<PracticeBest> findBestByUserId(@Param("userId") Long userId);
}
