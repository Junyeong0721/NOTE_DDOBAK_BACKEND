# 노트 공유 커뮤니티 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 회원이 노트를 공개(고정 복사본)하고, 다른 회원이 검색·과목으로 찾아 읽고, 좋아요·댓글·가져오기·신고를 한다.

**Architecture:** 백엔드 `community` 패키지에 공개본(`community_post`)과 좋아요·댓글·신고 표, 공개 API(`/api/notes/{id}/publish`)와 커뮤니티 API(`/api/community`). 프론트는 App view `community`(목록·보기)와 노트 안 공개 창.

**Tech Stack:** Spring Boot 4.1.2 + JPA + MySQL/Flyway(V5), 테스트 H2 MySQL 모드 / React 19 + Vite. 새 의존성 없음.

**Spec:** `docs/superpowers/specs/2026-11-22-community-design.md`

## Global Constraints
- 새 라이브러리 금지. 모든 API 로그인 필요(`@LoginUser`), 에러 메시지는 한국어
- V1~V4 SQL 수정 금지, `V5__community.sql` 추가 + 테스트 yaml `schema-locations`에 추가, `ddl-auto: validate` 통과
- 긴 글 칸은 `columnDefinition = "longtext"`, boolean은 SQL `BIT(1)`
- 개수 칸은 좋아요·댓글·가져오기·신고와 같은 트랜잭션에서 바꾼다
- 카드 응답에 content 넣지 않음. 페이지 크기 20, 과목 칩 15개, 댓글 최대 200개
- 커밋 작성자 `Junyeong0721` + Co-Authored-By / Claude-Session. 브랜치 `feat/community` (두 저장소)
- 검증: 백엔드 `/home/claude/tools/jtest .`, 프론트 `/home/claude/tools/mv src`, Playwright `/home/claude/smoke/`(가짜 서버 `server3.mjs`에 커뮤니티 API 추가)

## Review Focus
1. **두 번 누르기**: 좋아요 연타·동시 요청 → 개수는 실제 좋아요 줄 수와 같아야 함 (PUT/DELETE 멱등) → Task 2 테스트
2. **원본 노트 삭제·폴더 삭제 뒤**: 공개본은 남고 `GET /api/notes/{id}/publish`는 404, 커뮤니티에선 그대로 보임 → Task 1 테스트
3. **숨겨진 글 접근**: 목록·과목 개수·검색·보기·댓글·좋아요·가져오기 모두에서 남에게는 없는 글 → Task 2 테스트
4. **긴/이상한 입력**: 제목 101자, 과목 공백만, 검색어 51자·`%`·`_` (LIKE 이스케이프) → Task 1·2 테스트
5. **가져온 노트**: 원본 공개본이 나중에 바뀌어도 가져온 내 노트는 그대로, 요약은 없음 → Task 2 테스트

---

### Task 1: 공개본 표와 공개 API (백엔드)
**Files:** `V5__community.sql`, `community/CommunityPost.java`, `CommunityPostRepository.java`, `PublishService.java`, `PublishController.java`, `PublishRequest.java`, `PublishStatus.java`, 예외 4개, `GlobalExceptionHandler` 수정, 테스트 `community/PublishApiTest.java`, `community/CommunitySchemaTest.java`

**Produces:** `PublishStatus(boolean published, Long postId, String title, String subject, String intro, int likeCount, int commentCount, int copyCount, boolean hidden, boolean outdated, Instant publishedAt, Instant updatedAt)`, `PublishRequest(String title, String subject, String intro, boolean refreshContent)`; `CommunityPost.refreshContent(Note)`, `updateInfo(...)`

- [ ] 테스트: 빈 노트(획 0) 공개 400 "필기가 있어야 공개할 수 있어요." / 공개 → 상태 published·title·subject / 같은 노트 두 번 PUT은 정보 수정(공개본 1개) / 원본 수정 후 outdated true → `refreshContent:true`로 outdated false, 좋아요·댓글 수 유지(Task 2 뒤 보강) / 제목 101자·과목 공백·소개 201자 400 / DELETE 204 → published false / 남의 노트 404 / 원본 노트 삭제 → 공개본 남음(`note_id` NULL) / 회원 삭제 → 공개본 삭제 / V5 SQL에 `uk_post_note`, `ON DELETE SET NULL`, `content LONGTEXT` 포함
- [ ] 구현 (빈 노트 판단: content JSON의 pages[*].strokes가 모두 비었는지)
- [ ] `jtest .` 전부 통과 → 커밋 `feat: publish notes to the community`

### Task 2: 커뮤니티 API (백엔드)
**Files:** `PostLike`, `PostComment`, `PostReport`, `PostUserId`, 각 Repository, `CommunityService.java`, `CommunityController.java`, DTO(`PostCard`, `PostPage`, `PostDetail`, `CommentResponse`, `SubjectCount`, `LikeResponse`, `CopyResponse`, `CommentRequest`, `ReportRequest`), 테스트 `community/CommunityApiTest.java`

**Produces:** `PostCard(id, title, subject, intro, summaryLine, thumbnail, author, likeCount, commentCount, liked, mine, publishedAt)`, `PostPage(List<PostCard> items, boolean hasMore)`, `PostDetail(… + content, summary, copyCount, hidden, myReport, updatedAt)`, `CommentResponse(id, author, body, mine, canDelete, createdAt)`

- [ ] 테스트(두 계정): 최신순·인기순(좋아요 같으면 최신)·과목·검색(제목/과목/소개/요약, `%`·`_` 글자 그대로)·20개 페이지·hasMore / subjects 많은 순·숨김 제외 / 보기에 content·author 닉네임·liked·mine / 좋아요 PUT 두 번·DELETE 두 번 → 개수 정확, 자기 글 400 / 가져오기 → 내 노트 생김(제목 "(가져옴)", content 같음, summary 없음), copy_count+1, 이후 원본 업데이트해도 가져온 노트 그대로 / 댓글 1~500자, 삭제는 쓴 사람·글 주인만(그 외 403), comment_count / 신고 한 번(두 번째 409), 3명이면 hidden → 남에게 목록·보기·댓글·좋아요 404, 주인은 보임 / 로그인 없이 401
- [ ] 구현 (검색은 JPQL `lower(...) like lower(:q) escape '\'`, `%`·`_`·`\` 이스케이프)
- [ ] `jtest .` → 커밋 `feat: browse, like, copy, comment on and report shared notes`

### Task 3: 공개 창 (프론트)
**Files:** `src/api/community.js`(+test), `src/community/subjects.js`(+test), `src/components/PublishDialog.jsx`, NoteEditor에 [공개] 버튼, CSS
- [ ] 테스트: API 경로·메서드·본문, `suggestSubjects(folderName, communitySubjects)` 중복 제거·폴더 먼저·최대 8개
- [ ] 구현: 공개 전/공개 중 화면(spec 2.1), 공개 그만하기는 확인 한 번 더
- [ ] `mv src` → 커밋

### Task 4: 커뮤니티 목록·보기·댓글 (프론트)
**Files:** `src/components/community/*`, `src/community/format.js`(+test), App view `community`, Home 카드
- [ ] 테스트: `cardLine(post)`(요약 첫 줄 → 소개 → 빈 글), `countLabel(n)`(1,234 → "1.2천")
- [ ] 구현: spec 2.2·2.3. 읽기 전용 페이지는 `drawPage`로 페이지마다 캔버스(폭 맞춤), 가져오기 후 [열기]는 그 노트 편집기로
- [ ] `mv src`, esbuild 번들 → 커밋

### Task 5: 두 계정 흐름 확인·문서·PR
- [ ] 가짜 서버에 커뮤니티 API(메모리) + 계정 전환, Playwright: A 공개 → B 검색·과목·읽기·좋아요·댓글·가져오기 → A 수정·업데이트 → B 새 내용 / 신고 3명 숨김 / 폰 크기 스크린샷
- [ ] 개발 기록 `docs/dev-log/2026-11-22-community.md`, PR 두 개, PC로 bundle, 진행 상황 문서
