package kr.study.ddobak.community;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostReport {

    @EmbeddedId
    private PostUserId id;

    @Column(nullable = false, length = 20)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    PostReport(Long postId, Long userId, String reason) {
        this.id = new PostUserId(postId, userId);
        this.reason = reason;
        this.createdAt = Instant.now();
    }
}
