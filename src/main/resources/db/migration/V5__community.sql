-- V5: 노트 공유 커뮤니티 (2026-11-22)
-- 공개하면 그 순간의 필기를 복사해 둔다(community_post.content). 원본을 고쳐도 [공개본 업데이트] 전에는 그대로.
-- 좋아요·댓글·신고 수는 목록 정렬·표시용으로 공개본에도 저장하고, 바꿀 때 같은 트랜잭션에서 다시 센다.

CREATE TABLE community_post (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    user_id       BIGINT        NOT NULL COMMENT '공개한 회원',
    note_id       BIGINT        NULL     COMMENT '원본 노트. 노트를 지우면 NULL (공개본은 남음)',
    title         VARCHAR(100)  NOT NULL,
    subject       VARCHAR(30)   NOT NULL COMMENT '과목',
    intro         VARCHAR(200)  NULL     COMMENT '한 줄 소개',
    summary       VARCHAR(1000) NULL     COMMENT '공개(업데이트) 때 원본 노트의 AI 요약',
    content       LONGTEXT      NOT NULL COMMENT '공개 순간의 필기 복사본 JSON',
    thumbnail     LONGTEXT      NULL     COMMENT '첫 페이지 미리보기 PNG data URL',
    like_count    INT           NOT NULL DEFAULT 0,
    comment_count INT           NOT NULL DEFAULT 0,
    copy_count    INT           NOT NULL DEFAULT 0 COMMENT '내 노트로 가져간 횟수',
    report_count  INT           NOT NULL DEFAULT 0,
    hidden        BIT(1)        NOT NULL DEFAULT 0 COMMENT '신고 3번이면 1 (주인만 볼 수 있음)',
    published_at  DATETIME(6)   NOT NULL COMMENT '처음 공개한 때',
    updated_at    DATETIME(6)   NOT NULL COMMENT '공개본을 마지막으로 바꾼 때 (업데이트·정보 수정)',
    content_at    DATETIME(6)   NOT NULL COMMENT '필기 복사본을 만든 때. 원본 note.updated_at이 이보다 뒤면 "공개 후 바뀜"',
    PRIMARY KEY (id),
    CONSTRAINT uk_post_note UNIQUE (note_id),
    CONSTRAINT fk_post_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_note FOREIGN KEY (note_id) REFERENCES note (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_post_published ON community_post (hidden, published_at);
CREATE INDEX idx_post_likes ON community_post (hidden, like_count, published_at);
CREATE INDEX idx_post_subject ON community_post (subject);
CREATE INDEX idx_post_user ON community_post (user_id);

CREATE TABLE post_like (
    post_id    BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (post_id, user_id),
    CONSTRAINT fk_like_post FOREIGN KEY (post_id) REFERENCES community_post (id) ON DELETE CASCADE,
    CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE post_comment (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    post_id    BIGINT       NOT NULL,
    user_id    BIGINT       NOT NULL COMMENT '쓴 회원',
    body       VARCHAR(500) NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_comment_post FOREIGN KEY (post_id) REFERENCES community_post (id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_comment_post ON post_comment (post_id, created_at);

CREATE TABLE post_report (
    post_id    BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL COMMENT '신고한 회원 (한 글에 한 번)',
    reason     VARCHAR(20) NOT NULL COMMENT 'inappropriate / spam / other',
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (post_id, user_id),
    CONSTRAINT fk_report_post FOREIGN KEY (post_id) REFERENCES community_post (id) ON DELETE CASCADE,
    CONSTRAINT fk_report_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
