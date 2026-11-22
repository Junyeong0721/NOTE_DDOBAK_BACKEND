package kr.study.ddobak.community;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PostReportRepository extends JpaRepository<PostReport, PostUserId> {
}
