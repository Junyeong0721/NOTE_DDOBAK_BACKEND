# 노트 공유 커뮤니티 설계 (2026-11-22)

또박 회원끼리 필기 노트를 공개하고, 둘러보고, 좋아요·댓글을 달고, 내 노트로 가져온다.

## 1. 사용자와 정한 것

- 첫 버전에 넣는 것: 공개하기·둘러보기·읽기 + **좋아요, 내 노트로 가져오기, 댓글, 검색·과목 필터** (전부)
- **또박 회원만** 볼 수 있다 (로그인 필요, 링크 공개 없음)
- **공개하면 그 순간 모습으로 고정**(복사본). 내 노트를 고친 뒤 **[공개본 업데이트]**를 눌러야 바뀐다. 제목·과목·소개는 언제든 수정, [공개 그만하기]도 언제든
- 신고: 한 사람 한 번, **3명이 신고하면 자동으로 숨김**. 관리자 화면은 없음
- 원본 노트를 지워도 공개본은 남는다. 회원을 지우면 그 사람의 공개 노트·좋아요·댓글·신고도 지워진다

**성공 기준**: 아이디 2개로 — A가 노트를 공개 → B가 목록·검색·과목으로 찾고, 읽고, 좋아요·댓글·가져오기 → A가 고친 뒤 공개본 업데이트 → B에게 새 내용, 좋아요·댓글 유지.

**이번에 안 하는 것**: 알림, 팔로우, 관리자 화면, 링크 공개, 댓글 수정·답글, 이미지 첨부.

## 2. 화면

### 2.1 노트 안 [공개]
- 처음: 공개 창 — 제목(기본: 노트 제목, 100자), **과목**(30자, 필수. 추천: 이 노트의 폴더 이름 + 커뮤니티에 있는 과목), 한 줄 소개(200자, 선택). "AI 요약이 있으면 같이 공개돼요" 안내 → [공개하기]
- 공개 중: "공개 중 · ♥ N · 댓글 N · 가져감 N", 제목·과목·소개 고치기 [저장], 노트가 공개 뒤 바뀌었으면 "공개 후 노트가 바뀌었어요" + **[공개본 업데이트]**, 신고로 숨겨졌으면 "신고가 쌓여 숨겨졌어요", [공개 그만하기](확인 후 삭제 — 좋아요·댓글도 사라짐), [커뮤니티에서 보기]
- 빈 노트(획 0)는 공개할 수 없다: "필기가 있어야 공개할 수 있어요"

### 2.2 커뮤니티 (홈의 [커뮤니티], App view `community`)
- 검색창(제목·과목·소개·AI 요약에서 찾기), [최신순 | 인기순(좋아요 수, 같으면 최신)], 과목 칩(공개 노트가 있는 과목, 많은 순 15개)
- 카드: 썸네일, 제목, 과목 태그, 요약 첫 줄(없으면 소개), 닉네임, ♥ N, 댓글 N. 내 글이면 "내 노트" 표시
- 20개씩, [더 보기]. 결과 없으면 "조건에 맞는 노트가 없어요"

### 2.3 공개 노트 보기
- 위: 제목, 과목, 닉네임, 공개(업데이트) 날짜, 한 줄 소개, AI 요약
- 버튼: [♥ 좋아요 N](누르면 켜고 끔), [내 노트로 가져오기](→ "가져왔어요 [열기]"), [신고](이유: 부적절한 내용 / 광고·도배 / 기타, 한 번만), 내 글이면 신고 대신 [내 노트에서 공개 설정]
- 필기: 읽기 전용, 페이지를 세로로. 보정 글자는 각 보정에 저장된 글씨체로
- 아래: 댓글 목록(닉네임, 날짜, 내용) + 입력(500자). 내 댓글과 내 노트에 달린 댓글엔 [삭제]
- 숨겨진 글은 주인만 볼 수 있다(다른 회원에게는 404)

## 3. 데이터 — Flyway `V5__community.sql`

```sql
CREATE TABLE community_post (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    note_id       BIGINT       NULL     COMMENT '원본 노트. 노트를 지우면 NULL (공개본은 남음)',
    title         VARCHAR(100) NOT NULL,
    subject       VARCHAR(30)  NOT NULL COMMENT '과목',
    intro         VARCHAR(200) NULL     COMMENT '한 줄 소개',
    summary       VARCHAR(1000) NULL    COMMENT '공개(업데이트) 때 원본 노트의 AI 요약',
    content       LONGTEXT     NOT NULL COMMENT '공개 순간의 필기 복사본',
    thumbnail     LONGTEXT     NULL,
    like_count    INT          NOT NULL DEFAULT 0,
    comment_count INT          NOT NULL DEFAULT 0,
    copy_count    INT          NOT NULL DEFAULT 0,
    report_count  INT          NOT NULL DEFAULT 0,
    hidden        BIT(1)       NOT NULL DEFAULT 0 COMMENT '신고 3번이면 1',
    published_at  DATETIME(6)  NOT NULL COMMENT '처음 공개',
    updated_at    DATETIME(6)  NOT NULL COMMENT '공개본을 마지막으로 바꾼 때 (내용 업데이트·정보 수정)',
    content_at    DATETIME(6)  NOT NULL COMMENT '필기 복사본을 마지막으로 만든 때 — 원본 note.updated_at이 이보다 뒤면 "바뀜"',
    PRIMARY KEY (id),
    CONSTRAINT uk_post_note UNIQUE (note_id),
    CONSTRAINT fk_post_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_post_note FOREIGN KEY (note_id) REFERENCES note (id) ON DELETE SET NULL
);
CREATE INDEX idx_post_published ON community_post (hidden, published_at);
CREATE INDEX idx_post_likes ON community_post (hidden, like_count, published_at);
CREATE INDEX idx_post_subject ON community_post (subject);

CREATE TABLE post_like (post_id, user_id, created_at, PK (post_id, user_id), FK 둘 다 CASCADE);
CREATE TABLE post_comment (id, post_id, user_id, body VARCHAR(500), created_at, FK 둘 다 CASCADE, INDEX (post_id, created_at));
CREATE TABLE post_report (post_id, user_id, reason VARCHAR(20), created_at, PK (post_id, user_id), FK 둘 다 CASCADE);
```
- 개수(like/comment/copy/report_count)는 목록 정렬·표시를 빠르게 하려고 같이 저장하고, 좋아요·댓글을 바꿀 때 같은 트랜잭션에서 함께 고친다
- 테스트 yaml `schema-locations`에 V5 추가

## 4. API (모두 로그인 필요)

| 주소 | 하는 일 |
|---|---|
| `GET /api/notes/{id}/publish` | 이 노트의 공개 상태: `{ published: false }` 또는 `{ published: true, postId, title, subject, intro, likeCount, commentCount, copyCount, hidden, outdated, publishedAt, updatedAt }` |
| `PUT /api/notes/{id}/publish` `{ title, subject, intro, refreshContent }` | 처음이면 공개(201 대신 200), 이미 공개면 정보 수정. `refreshContent: true`면 필기 복사본·썸네일·요약도 지금 노트로 바꿈(처음 공개는 항상) |
| `DELETE /api/notes/{id}/publish` | 공개 그만하기 (204) |
| `GET /api/community?sort=latest\|popular&subject=&q=&page=0` | 카드 20개 + `hasMore` |
| `GET /api/community/subjects` | `[{ subject, count }]` 많은 순 15개 (숨김 제외) |
| `GET /api/community/{postId}` | 공개 노트 하나 (content 포함, author, liked, mine, myReport) |
| `PUT` / `DELETE /api/community/{postId}/like` | 좋아요 켜기/끄기 → `{ liked, likeCount }` (여러 번 눌러도 안전) |
| `POST /api/community/{postId}/copy` | 내 노트로 가져오기 → 새 노트 `{ noteId }` (제목 "원래 제목 (가져옴)", 미분류, 요약은 안 가져옴) |
| `GET /api/community/{postId}/comments` | 댓글 전체(오래된 순, 최대 200) |
| `POST /api/community/{postId}/comments` `{ body }` | 댓글 쓰기(1~500자) → 201 |
| `DELETE /api/community/{postId}/comments/{id}` | 내 댓글 또는 내 글의 댓글만 → 204, 아니면 403 |
| `POST /api/community/{postId}/report` `{ reason }` | 신고(한 번만, 두 번째는 409). 3번째 신고에서 `hidden = 1` |

- 숨겨진 글·없는 글: 주인이 아니면 404. 자기 글은 좋아요·신고 불가(400)
- 검색 `q`: 앞뒤 공백 제거 후 1~50자, 제목·과목·소개·요약에 포함(대소문자 무시). `subject`는 정확히 일치
- 카드에는 content를 넣지 않는다(크기)

## 5. 코드 구조

**백엔드** `kr.study.ddobak.community`: `CommunityPost`(엔티티), `PostLike`, `PostComment`, `PostReport`(+ 복합키 `PostUserId`), 각 Repository, `PublishService`(공개·수정·그만하기·상태), `CommunityService`(목록·보기·좋아요·가져오기·댓글·신고), `PublishController`(`/api/notes/{id}/publish`), `CommunityController`(`/api/community`), DTO, 예외(`PostNotFoundException` 404, `ForbiddenException` 403, `AlreadyReportedException` 409, `InvalidCommunityException` 400). 노트 생성은 `NoteService`를 재사용

**프론트**
- `src/api/community.js`
- `src/components/community/`: `Community.jsx`(목록·보기 전환), `CommunityList.jsx`, `CommunityPost.jsx`, `ReadOnlyPages.jsx`(drawPage로 페이지 그리기), `Comments.jsx`, `Community.css`
- `src/components/PublishDialog.jsx` (노트 안 [공개])
- `src/community/` 순수 로직: `subjects.js`(과목 추천 합치기), `format.js`(카드 한 줄 글, 숫자 표시)
- App view `community`, Home에 [커뮤니티] 카드, NoteEditor 위쪽 [공개] 버튼

## 6. 테스트
- 서버: 공개(빈 노트 거절)·정보 수정·공개본 업데이트(좋아요·댓글 유지)·outdated 판단·그만하기, 원본 노트 삭제 후에도 공개본 유지, 목록 정렬·과목·검색·페이지, 좋아요 켜기/끄기 두 번 눌러도 개수 정확, 가져오기(새 노트 생성·copy_count), 댓글 쓰기·삭제 권한(403), 신고 한 번만(409)·3번째 숨김·주인만 보임, 회원 삭제 시 함께 삭제, 로그인 없으면 401, V5 표 validate
- 화면: Playwright로 두 계정 흐름(공개 → 다른 계정이 검색·과목·읽기·좋아요·댓글·가져오기 → 주인 업데이트), 폰 크기
