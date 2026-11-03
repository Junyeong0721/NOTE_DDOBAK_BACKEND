# 노트 기능 보강 + 디자인 개편 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 필기 도구(펜 굵기·형광펜, 올가미 선택, 확대·축소)와 노트 관리(과목별 폴더, 썸네일 목록, 제목 검색)를 추가하고, "형광펜과 바인더 인덱스" 디자인으로 목록·에디터 화면을 다시 만든다.

**Architecture:** 백엔드에 `Folder`(이름, 색)와 노트의 `folderId`·`thumbnail`을 추가한다. 프론트의 새 동작(형광펜, 올가미, 확대 계산, 목록 필터)은 순수 함수 모듈로 만들고 테스트로 고정한 뒤, 화면 컴포넌트가 이를 쓴다. 필기 데이터의 "원본 불변 + transform" 원칙은 유지하되, 올가미로 **옮기기**는 사용자의 의도이므로 원본 좌표를 이동한다.

**Tech Stack:** React 19 + Vite 8, Canvas 2D, Pointer Events / Spring Boot 4.1.2, Java 21, JPA, H2

**Spec:** 기획서 v0.6 3장 "노트 기본 기능"·"필기 페이지와 글씨·그림 구분", 사용자 결정(2026-11-03): 펜 굵기·형광펜, 올가미 선택, 확대·축소, 목록 썸네일·검색, 폴더(과목별), 디자인 "밝고 컬러풀"

## Global Constraints

- 저장소 경로(새 PC): `D:\WORKSPACE\또박\NOTE_DDOBAK_BACKEND`, `D:\WORKSPACE\또박\NOTE_DDOBAK_FRONTEND`. Claude는 GitHub에 push, 사용자는 `git pull`.
- 1·2주차 규칙 유지: 페이지 좌표 1000×1414, 오류 본문 `{"message"}`, 백엔드 8081·127.0.0.1, 펜·마우스만 그리기.
- Stroke에 선택 필드 추가: `tool: "pen" | "highlighter"`(없으면 pen), `kind: "text" | "drawing"`(올가미로 직접 지정한 경우만, 없으면 자동 판정). 1·2주차 노트는 그대로 열려야 한다.
- 형광펜 획: 반투명(알파 0.35)으로 펜 획 **아래**에 그린다. 줄 묶기·OCR 대상이 아니다. 노트 정리 시 가장 많이 겹치는 글씨 줄과 같은 transform으로 함께 움직인다.
- 펜 굵기: 가늘게 1.6 / 보통 2.4 / 굵게 4.0. 형광펜 굵기: 12 / 18 / 26. 펜 색: `#1f2a44`(남색 잉크), `#2563eb`, `#e5484d`, `#16a34a`. 형광펜 색: `#ffd84d`, `#ff7aa8`, `#3ccfa8`, `#4da8ff`.
- 폴더 색 키: `yellow #ffd84d`, `pink #ff7aa8`, `mint #3ccfa8`, `sky #4da8ff`, `orange #ff9f43`, `lavender #9b8cff`. 폴더 이름 1~30자.
- 확대: 0.75~2.5배. 캔버스 픽셀 수는 iPad 한도(약 1,600만)를 넘지 않게 기기 픽셀 비율을 낮춘다.
- 썸네일: 첫 페이지를 가로 240px PNG data URL로, 노트 저장 때 함께 보낸다. 최대 300,000자.
- 디자인 토큰: 배경 `#f3f6fb`, 종이 `#ffffff`, 잉크 `#1f2a44`, 보조 글자 `#5b6478`, 선 `#dfe4ee`. 본문 IBM Plex Sans KR, 로고만 Nanum Pen Script(둘 다 Google Fonts, OFL). 그림자 대신 얇은 선, 모서리 둥글기는 위계별로(버튼 10px, 썸네일 6px, 시트 16px).

## Review Focus

1. 1·2주차에 만든 노트(`tool`, `kind`, `thumbnail`, `folderId` 없음) → 목록·에디터에서 오류 없이 열리고, 썸네일 자리에 빈 종이가 보인다 — Task 2, 6 테스트
2. 폴더를 지우면 → 그 폴더의 노트는 지워지지 않고 "미분류"로 간다 — Task 1 테스트
3. 형광펜으로 글씨를 칠한 뒤 노트 정리 → 형광펜이 글씨와 함께 움직이고, 형광펜 때문에 줄 묶기가 바뀌지 않는다 — Task 3 테스트
4. 노트 정리된 획을 올가미로 옮긴 뒤 다시 정리 → 획이 옮긴 자리 근처에 남고 엉뚱한 곳으로 튀지 않는다 — Task 4 테스트
5. 2.5배 확대 상태에서 쓰기 → 좌표가 어긋나지 않고, 캔버스가 너무 커서 하얗게 비는 일이 없다 — Task 5 테스트 + iPad 확인

---

### Task 1: 백엔드 — 폴더 API

**Files:** Create `src/main/java/kr/study/ddobak/folder/{Folder, FolderColor, FolderRepository, FolderRequest, FolderResponse, FolderService, FolderController, FolderNotFoundException}.java`; Modify `common/GlobalExceptionHandler.java`, `note/NoteRepository.java`; Test `src/test/java/kr/study/ddobak/folder/FolderApiTest.java`

**Interfaces:**
- `enum FolderColor { yellow, pink, mint, sky, orange, lavender }`
- `record FolderRequest(@NotBlank @Size(max = 30) String name, @NotNull FolderColor color)` — 메시지 `"폴더 이름을 입력해 주세요."`, `"폴더 이름은 30자 이하로 입력해 주세요."`, `"폴더 색을 골라 주세요."`
- `record FolderResponse(Long id, String name, FolderColor color, Instant createdAt)`
- `GET /api/folders`(만든 순) · `POST /api/folders` 201 · `PUT /api/folders/{id}` · `DELETE /api/folders/{id}` 204 — 삭제 시 그 폴더 노트의 `folderId`를 `null`로
- `FolderNotFoundException(Long id)` → 404 `"폴더를 찾을 수 없습니다. id={id}"`
- `NoteRepository`: `@Modifying @Query("update Note n set n.folderId = null where n.folderId = :folderId") int unassignFolder(Long folderId)`

- [ ] Step 1: 실패하는 테스트 — `create_returns201`, `list_inCreatedOrder`, `create_blankName_returns400`, `create_name31Chars_returns400`, `create_unknownColor_returns400`(`"neon"` → `"요청 형식이 올바르지 않습니다."`), `update_changesNameAndColor`, `update_unknown_returns404`, `delete_returns204`, `delete_movesNotesToUnfiled`(폴더에 노트 생성 → 폴더 삭제 → 노트 `folderId` null, 노트는 남음)
- [ ] Step 2: 실패 확인 → `.\gradlew.bat test --tests "*FolderApiTest"` 컴파일 에러
- [ ] Step 3: 구현 (Task 2의 `Note.folderId` 필드가 필요하므로 Task 2 Step 3과 함께 구현)
- [ ] Step 4: 통과 확인 → 전체 PASS
- [ ] Step 5: Commit `feat: add folders API`

### Task 2: 백엔드 — 노트의 폴더·썸네일

**Files:** Modify `note/{Note, NoteRequest, NoteResponse, NoteSummary, NoteService, NoteController}.java`; Create `note/NoteFolderRequest.java`, `note/InvalidThumbnailException.java`; Test `NoteApiTest.java`

**Interfaces:**
- `Note`: `Long folderId`(nullable, 인덱스), `@Lob String thumbnail`(nullable)
- `NoteRequest(title, content, Long folderId, String thumbnail)` — `folderId`는 **생성 때만** 쓰고 수정 때는 무시. `thumbnail`이 `null`이면 기존 값 유지. 있으면 `data:image/png;base64,`로 시작하고 300,000자 이하, 아니면 400 `"썸네일 형식이 올바르지 않습니다."`
- 생성 때 없는 `folderId` → 404 `"폴더를 찾을 수 없습니다. id=…"`
- `NoteResponse`, `NoteSummary`에 `folderId`, `thumbnail` 추가
- `PATCH /api/notes/{id}/folder` 본문 `record NoteFolderRequest(Long folderId)` → 200 `NoteSummary`(`null` = 미분류)

- [ ] Step 1: 실패하는 테스트 — `create_withFolder_returnsFolderId`, `create_unknownFolder_returns404`, `update_keepsFolder`, `update_withThumbnail_listShowsIt`, `update_nullThumbnail_keepsPrevious`, `update_invalidThumbnail_returns400`, `moveToFolder_changesFolder`, `moveToFolder_null_unfiles`, `moveToFolder_unknownFolder_returns404`, `moveToFolder_unknownNote_returns404`
- [ ] Step 2: 실패 확인
- [ ] Step 3: 구현
- [ ] Step 4: 통과 확인 → `.\gradlew.bat test` 전체 PASS (1·2주차 26개 포함)
- [ ] Step 5: Commit `feat: add folder and thumbnail to notes`

### Task 3: 프론트 — 펜 굵기·형광펜

**Files:** Create `src/note/tools.js`, `src/note/tools.test.js`; Modify `src/note/render.js`(+test), `src/note/lines.js`(+test), `src/note/tidy.js`(+test), `src/note/content.js`(+test)

**Interfaces:**
- `tools.js`: `PEN_COLORS`, `HIGHLIGHTER_COLORS`, `PEN_SIZES = { thin: 1.6, normal: 2.4, bold: 4.0 }`, `HIGHLIGHTER_SIZES = { thin: 12, normal: 18, bold: 26 }`, `HIGHLIGHTER_ALPHA = 0.35`, `isHighlighter(stroke)`
- `createStroke({ t0, color, width, tool })` — `tool`이 `'highlighter'`일 때만 필드 저장
- `drawPage`: 형광펜 획을 먼저(알파 0.35, 필압 무시 고정 굵기), 펜 획을 나중에
- `groupLines`: 형광펜 획 제외
- `tidyPage`: 형광펜 획은 세로로 가장 많이 겹치는 text 줄의 transform을 따라감(겹치는 줄 없으면 `null`)

- [ ] Step 1: 실패하는 테스트 — `highlighter strokes are not grouped into lines`, `highlighter follows the text line it covers when tidying`, `highlighter with no text line under it stays put`, `drawPage draws highlighters before pens with alpha 0.35`, `createStroke stores tool only for highlighter`, `old strokes without tool are pens`
- [ ] Step 2: 실패 확인 → `mv src/note`
- [ ] Step 3: 구현 → Step 4: 통과 → Step 5: Commit `feat: add pen sizes and highlighter`

### Task 4: 프론트 — 올가미 선택

**Files:** Create `src/note/lasso.js`, `src/note/lasso.test.js`; Modify `src/note/lines.js`(+test)

**Interfaces:**
- `pointInPolygon([x, y], polygon) → boolean`
- `selectStrokes(strokes, polygon) → string[]` — 변환 적용 위치 기준, 점의 절반 이상이 안에 있는 획
- `selectionBBox(strokes, ids) → bbox | null`
- `moveStrokes(strokes, ids, dx, dy) → Stroke[]` — 화면상 (dx, dy)만큼 보이게: transform 회전이 있으면 역회전한 이동량을 원본 점에 더한다. transform은 유지
- `deleteStrokes(strokes, ids)`, `setStrokesKind(strokes, ids, kind | null)`
- `groupLines`: `kind: 'drawing'` 획은 그림으로, `kind: 'text'` 획은 크기와 상관없이 글씨로

- [ ] Step 1: 실패하는 테스트 — `pointInPolygon inside/outside/concave`, `selects strokes mostly inside the lasso`, `ignores strokes mostly outside`, `selection uses transformed positions`, `moveStrokes moves plain strokes`, `moveStrokes on a rotated stroke moves it by the on-screen amount`, `moved tidy stroke stays near its new place after tidy again`, `deleteStrokes removes only selected`, `setStrokesKind drawing makes a text-sized stroke a drawing`, `setStrokesKind text keeps a big stroke as text`
- [ ] Step 2~5 → Commit `feat: add lasso selection model`

### Task 5: 프론트 — 확대·축소 계산

**Files:** Create `src/note/zoom.js`, `src/note/zoom.test.js`

**Interfaces:**
- `MIN_ZOOM = 0.75`, `MAX_ZOOM = 2.5`, `MAX_CANVAS_PIXELS = 16_000_000`
- `clampZoom(z)`
- `zoomAround({ zoom, nextZoom, scrollLeft, scrollTop, cx, cy }) → { scrollLeft, scrollTop }` — 손가락 가운데(cx, cy: 스크롤 영역 기준 화면 좌표)가 같은 곳을 가리키도록
- `effectiveDpr(cssWidth, cssHeight, dpr) → number` — `cssW × cssH × dpr²`가 한도를 넘으면 낮춤
- `pinchDistance(a, b)`

- [ ] Step 1: 실패하는 테스트 — 경계값(0.5→0.75, 3→2.5), 2배 확대 시 가운데 고정, 큰 캔버스에서 dpr 낮춤·작은 캔버스는 그대로
- [ ] Step 2~5 → Commit `feat: add zoom math`

### Task 6: 프론트 — API·목록 필터·썸네일

**Files:** Create `src/api/folders.js`(+test), `src/note/filterNotes.js`(+test), `src/note/thumbnail.js`; Modify `src/api/notes.js`(+test)

**Interfaces:**
- `listFolders()`, `createFolder({ name, color })`, `updateFolder(id, { name, color })`, `deleteFolder(id)`
- `moveNoteToFolder(id, folderId)` → `PATCH /api/notes/{id}/folder`
- `createNote({ title, content, folderId })`, `updateNote(id, { title, content, thumbnail })`
- `FOLDER_COLORS` (키 → hex), `UNFILED = 'unfiled'`, `ALL = 'all'`
- `filterNotes(notes, { folder, query }) → Note[]` — folder: `ALL` | `UNFILED` | 폴더 id. query: 제목에 포함(대소문자·앞뒤 공백 무시)
- `countByFolder(notes) → { all, unfiled, [folderId]: n }`
- `renderThumbnail(page) → 'data:image/png;base64,…'` (가로 240, 흰 배경, 형광펜 포함)

- [ ] Step 1: 실패하는 테스트 — API 경로·메서드·본문, `filterNotes` 조합(폴더+검색), 공백 검색은 전체, `countByFolder`
- [ ] Step 2~5 → Commit `feat: add folders client, note filtering and thumbnails`

### Task 7: 프론트 — 디자인 토큰 + 목록 화면

**Files:** Modify `index.html`(Google Fonts), `src/index.css`(토큰), `src/App.jsx`, `src/App.css`; Create `src/components/Icons.jsx`, `src/components/FolderSidebar.jsx`, `src/components/NoteGrid.jsx`, `src/components/FolderDialog.jsx`, `src/components/Logo.jsx`

- 넓은 화면: 왼쪽 폴더 사이드바(인덱스 탭: 왼쪽에 폴더 색 띠, 선택 시 띠가 넓어지고 연한 색 배경, 노트 수) + 오른쪽 상단 검색·새 노트, 아래 썸네일 격자(A4 비율 종이 + 아래 제목·날짜)
- 좁은 화면(세로 iPad·폰): 폴더가 위쪽 가로 스크롤 칩으로
- 노트 메뉴: 폴더 이동, 삭제. 폴더 메뉴: 이름·색 바꾸기, 삭제(노트는 미분류로 간다고 안내)
- 빈 화면 문구: 폴더가 비었을 때 "이 폴더에 노트가 없어요. 새 노트를 만들면 여기에 들어가요.", 검색 결과 없을 때 "‘{검색어}’와 맞는 노트가 없어요."
- 새 노트는 지금 선택한 폴더에 만든다

- [ ] Step 1: 구현 → Step 2: 사용자 PC `npm test`, `npm run lint` → Step 3: Claude 쪽 브라우저 스크린샷으로 디자인 점검(넓은·좁은 화면) → Step 4: Commit `feat: redesign note list with folders, thumbnails and search`

### Task 8: 프론트 — 에디터 개편

**Files:** Modify `src/components/NoteEditor.jsx`, `NoteEditor.css`, `NoteCanvas.jsx`; Create `src/components/ToolPalette.jsx`, `src/components/SelectionBar.jsx`

- 상단 바: 목록(폴더 색 탭 + 폴더 이름), 제목, 저장 상태, 실행 취소·다시 실행
- 도구 팔레트: 펜 / 형광펜 / 지우개 / 올가미, 선택한 도구의 색 4개와 굵기 3개, 보기(가이드 줄·줄 보기), 노트 정리
- 확대 표시(오른쪽 아래, 누르면 100%), 두 손가락 핀치 확대·축소, 한 손가락 상하좌우 이동, PC는 Ctrl+휠
- 올가미: 펜으로 둘러 선택 → 선택 영역 위 작은 막대(삭제, 그림으로 지정 / 글씨로 지정). 선택 영역 안을 펜으로 끌면 이동
- 저장 시 썸네일 함께 전송

- [ ] Step 1: 구현 → Step 2: 사용자 PC 테스트·린트 → Step 3: 브라우저 스모크(도구 전환, 형광펜, 올가미 이동·삭제, Ctrl+휠 확대, 저장 본문에 thumbnail) + 스크린샷 → Step 4: iPad 확인(핀치, 확대 상태 필기 좌표, 형광펜이 글씨 아래) → Step 5: Commit `feat: redesign editor with tool palette, lasso and zoom`

### Task 9: 개발 실행 스크립트를 저장소에

**Files:** Create `scripts/start-dev.bat` (프론트 저장소)

- 프론트 저장소 위치 기준으로 백엔드 폴더를 찾는다: `..\NOTE_DDOBAK_BACKEND` 또는 `..\ddobak-backend-springboot`. 나머지는 2주차 `start-dev.bat`과 같다(포트 점검 → 백엔드·프론트 실행 → Cloudflare 터널). cp949로 저장.

- [ ] Step 1: 작성 → Step 2: 사용자 PC 더블클릭 확인 → Step 3: Commit `chore: add dev start script to the repo`
