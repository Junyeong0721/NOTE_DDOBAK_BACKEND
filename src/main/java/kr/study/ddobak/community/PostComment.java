package kr.study.ddobak.community;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "post_comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 500)
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    PostComment(Long postId, Long userId, String body) {
        this.postId = postId;
        this.userId = userId;
        this.body = body;
        this.createdAt = Instant.now();
    }
}
