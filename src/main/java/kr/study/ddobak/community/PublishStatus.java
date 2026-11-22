package kr.study.ddobak.community;

import java.time.Instant;

/** 노트 하나의 공개 상태. outdated: 공개본을 만든 뒤 원본 노트가 바뀜 */
public record PublishStatus(boolean published, Long postId, String title, String subject, String intro,
                            int likeCount, int commentCount, int copyCount, boolean hidden, boolean outdated,
                            Instant publishedAt, Instant updatedAt) {

    static PublishStatus none() {
        return new PublishStatus(false, null, null, null, null, 0, 0, 0, false, false, null, null);
    }
}
