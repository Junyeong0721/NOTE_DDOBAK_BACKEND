package kr.study.ddobak.community;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;

/** 좋아요·신고의 키: 한 회원이 한 글에 하나 */
@Embeddable
public record PostUserId(
        @Column(name = "post_id", nullable = false) Long postId,
        @Column(name = "user_id", nullable = false) Long userId) implements Serializable {
}
