-- V4: AI 노트 요약 (2026-11-16)
-- 노트 페이지 그림을 Gemini가 읽고 만든 핵심 요약. 제목 추천은 저장하지 않는다(사용자가 고르면 제목으로 바뀜)

ALTER TABLE note ADD COLUMN summary VARCHAR(1000) NULL COMMENT 'AI 요약 3~5줄 (줄바꿈으로 이어 붙임). 아직 안 만들었으면 NULL';
ALTER TABLE note ADD COLUMN summarized_at DATETIME(6) NULL COMMENT '요약을 만든 시각. updated_at이 이보다 뒤면 요약 뒤에 노트가 바뀐 것';
