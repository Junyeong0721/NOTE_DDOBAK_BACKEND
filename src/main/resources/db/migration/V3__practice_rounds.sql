-- V3: 글씨 연습을 "한 판 = 랜덤 문제 5개"로 바꿈 (2026-11-14)
-- 세트를 고르던 방식(V2)의 기록은 모양이 달라서 지운다. (머지 전 시험용 기록뿐)

DELETE FROM practice_attempt;

ALTER TABLE practice_attempt DROP INDEX idx_practice_user_text;
ALTER TABLE practice_attempt DROP COLUMN set_id;
ALTER TABLE practice_attempt ADD COLUMN stage VARCHAR(20) NOT NULL COMMENT '단계: syllable(글자) / word(단어) / sentence(문장)';
ALTER TABLE practice_attempt MODIFY COLUMN target_text VARCHAR(300) NOT NULL COMMENT '한 판에 나온 문제들 (줄바꿈으로 이어 붙임)';
ALTER TABLE practice_attempt MODIFY COLUMN strokes LONGTEXT NOT NULL COMMENT '문제별 글·칸별 획·글자별 점수 JSON (다시 보기용)';
ALTER TABLE practice_attempt MODIFY COLUMN char_scores LONGTEXT NOT NULL COMMENT '문제별 점수 JSON';
ALTER TABLE practice_attempt MODIFY COLUMN total_score INT NOT NULL COMMENT '판 점수 0~100 (문제 점수의 평균)';

-- 단계마다 최고 점수를 찾는 쿼리용
CREATE INDEX idx_practice_user_stage ON practice_attempt (user_id, stage);
