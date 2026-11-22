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
@Table(name = "post_like")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike {

    @EmbeddedId
    private PostUserId id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    PostLike(Long postId, Long userId) {
        this.id = new PostUserId(postId, userId);
        this.createdAt = Instant.now();
    }
}
