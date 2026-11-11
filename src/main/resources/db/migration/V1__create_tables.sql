-- V1: 또박 첫 표 설계 — 회원(users), 과목 폴더(folder), 노트(note)
-- Flyway가 한 번만 실행하고 flyway_schema_history 표에 기록한다. 이미 실행한 파일은 고치지 말고 V2__...sql을 새로 만든다.
-- 시각은 모두 UTC (DATETIME(6), 마이크로초까지)

CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(254) NULL     COMMENT '로그인 이메일(소문자로 저장). 소셜 가입은 없을 수 있음',
    nickname      VARCHAR(20)  NOT NULL COMMENT '홈 화면에서 부르는 이름',
    password_hash VARCHAR(200) NULL     COMMENT 'pbkdf2$반복횟수$소금$해시. 소셜 가입은 NULL, 원문 비밀번호는 저장하지 않음',
    provider      VARCHAR(10)  NOT NULL COMMENT '가입 방식: LOCAL, GOOGLE, KAKAO',
    provider_id   VARCHAR(100) NULL     COMMENT '소셜 로그인 서비스의 회원 번호',
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_provider UNIQUE (provider, provider_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE folder (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL COMMENT '주인 회원',
    name       VARCHAR(30) NOT NULL COMMENT '과목 이름',
    color      VARCHAR(20) NOT NULL COMMENT '인덱스 탭 색: yellow, pink, mint, sky, orange, lavender',
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_folder_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE note (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL COMMENT '주인 회원',
    folder_id  BIGINT       NULL     COMMENT '과목 폴더. NULL이면 미분류',
    title      VARCHAR(100) NOT NULL,
    content    LONGTEXT     NOT NULL COMMENT '필기 데이터 JSON (페이지·획·보정). 획이 많으면 수 MB',
    thumbnail  LONGTEXT     NULL     COMMENT '목록용 첫 페이지 미리보기 PNG data URL',
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_note_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    -- 폴더를 지우면 노트는 남기고 미분류로 (앱에서도 먼저 옮기지만 DB가 한 번 더 지킨다)
    CONSTRAINT fk_note_folder FOREIGN KEY (folder_id) REFERENCES folder (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 내 노트 목록을 최근 수정 순으로 가져오는 쿼리용
CREATE INDEX idx_note_user_updated ON note (user_id, updated_at);
