-- V2: 글씨 연습 기록(practice_attempt)
-- 연습 한 세트를 채점할 때마다 한 줄. 점수는 앱(브라우저)이 계산해서 보내고, 조언은 AI가 붙인다.

CREATE TABLE practice_attempt (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    user_id        BIGINT       NOT NULL COMMENT '주인 회원',
    set_id         VARCHAR(40)  NULL     COMMENT '미리 넣어 둔 세트 이름(예: consonant-1). 직접 입력이면 NULL',
    target_text    VARCHAR(30)  NOT NULL COMMENT '연습한 글 (띄어쓰기 포함 30자까지)',
    font_id        VARCHAR(40)  NOT NULL COMMENT '연습한 글씨체',
    guide          BIT(1)       NOT NULL COMMENT '가이드(연한 목표 글자)를 켜고 썼는지: 1 = 켬',
    strokes        LONGTEXT     NOT NULL COMMENT '칸별로 내가 쓴 획 JSON (다시 보기용)',
    char_scores    LONGTEXT     NOT NULL COMMENT '글자별 점수 JSON',
    total_score    INT          NOT NULL COMMENT '세트 점수 0~100',
    shape_score    INT          NOT NULL COMMENT '모양 점수 0~100 (%)',
    position_score INT          NOT NULL COMMENT '위치·크기 점수 0~100 (%)',
    tilt_score     INT          NOT NULL COMMENT '기울기 점수 0~100 (%)',
    advice         VARCHAR(300) NULL     COMMENT 'AI 조언. 받지 못했으면 NULL',
    created_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_practice_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 내 최근 연습 목록용
CREATE INDEX idx_practice_user_created ON practice_attempt (user_id, created_at);
-- 글마다 최고 점수를 찾는 쿼리용
CREATE INDEX idx_practice_user_text ON practice_attempt (user_id, target_text);
