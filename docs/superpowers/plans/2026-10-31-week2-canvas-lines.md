# 2주차: 필기 캔버스 + 줄 묶기 + 노트 정리 + PWA Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** iPad에서 Apple Pencil로 무지 페이지에 글씨와 그림을 쓰고 저장·불러오며, 쓴 글씨를 줄 단위로 묶어 한 줄이 끝날 때 OCR(가짜 응답)을 호출하고, "노트 정리"로 글씨 줄만 반듯하게 정돈한다. 홈 화면에 추가하면 앱처럼 열린다.

**Architecture:** 필기 데이터는 노트의 `content` JSON(1주차 API 그대로)에 페이지 좌표로 저장한다. 줄 묶기·글씨/그림 판정·노트 정리는 프론트의 순수 함수(`src/note/*.js`)로 만들고, 원본 좌표는 바꾸지 않고 획별 `transform`만 쓴다. 줄 정보는 저장하지 않고 매번 계산한다. 백엔드는 `content` 형식 검사를 강화하고, 3주차에 Google Cloud Vision으로 바꿀 `OcrClient` 인터페이스와 가짜 구현을 둔다.

**Tech Stack:** React 19 + Vite 8, Canvas 2D + Pointer Events, Vitest / Spring Boot 4.1.2, Java 21

**Spec:** 기획서 v0.6 — https://claude.ai/artifact/37fZAQdu7t8TMxbCMRu1JV (3장 "필기 페이지와 글씨·그림 구분", 6장 "글자 보정 단계" 1단계, 8장 2주차)

## Global Constraints

- 페이지 좌표계: 가로 `1000`, 세로 `1414`(A4 비율). 화면 픽셀이 아니라 이 좌표로 저장한다.
- `content` 형식: `{"version":1,"page":{"width":1000,"height":1414},"pages":[{"strokes":[Stroke...]}]}`. `page`가 없는 1주차 노트도 열려야 한다(기본값 적용).
- Stroke: `{"id","t0","color","width","points":[[x,y,pressure,ms]...],"transform":null|{"dx","dy","rotate","ox","oy"}}`. x·y는 소수 1자리, pressure는 소수 2자리(0이면 0.5), ms는 `t0` 기준 정수.
- `transform` 적용 순서: 점을 (ox, oy) 중심으로 `rotate`(라디안) 회전한 뒤 (dx, dy) 이동.
- 원본 `points`는 노트 정리·보정이 절대 바꾸지 않는다. 정리 취소 = `transform`을 `null`로.
- 펜 입력만 그린다: `pointerType`이 `pen` 또는 `mouse`(PC 테스트용)일 때만 그리고, `touch`(손가락·손바닥)는 페이지 스크롤에만 쓴다.
- 백엔드 포트 8081, `127.0.0.1` 바인딩, 오류 본문 `{"message"}` (1주차 그대로).
- 프론트 테스트는 Vitest 문법으로 쓴다. Claude 쪽에서는 `scripts/mini-vitest.mjs`(Claude 작업 환경 전용, 저장소에 넣지 않음)로 순수 함수 테스트를 돌려 RED→GREEN을 확인하고, 사용자 PC에서 `npm test`로 최종 확인한다.

## Review Focus

1. 1주차에 만든 노트(`page` 없음, 빈 strokes) → 에디터에서 오류 없이 열리고 저장된다 — Task 3 테스트
2. 손바닥이 화면에 닿은 채 Pencil로 쓰기 → 손바닥 터치로 획이 생기지 않는다 — Task 6 iPad 수동 확인
3. 글씨 옆에 큰 그림(동그라미, 화살표, 긴 밑줄) → 그림은 글씨 줄로 묶이지 않고, 노트 정리에서 움직이지 않는다 — Task 4, 5 테스트
4. 노트 정리를 두 번 연속 실행 → 결과가 같다(누적 회전 없음), 되돌리기 → 원래 위치 — Task 5 테스트
5. 획 수천 개(1시간 필기)인 노트 → 저장 요청이 1MB를 넘어도 실패하지 않고, 그리기가 끊기지 않는다 — 1주차 대용량 테스트 + Task 6 수동 확인

---

### Task 1: 백엔드 — `content` 형식 검사 강화

**Files:**
- Modify: `src/main/java/kr/study/ddobak/note/NoteService.java`
- Test: `src/test/java/kr/study/ddobak/note/NoteApiTest.java`

**Interfaces:**
- Produces: `content`는 JSON 객체이고 숫자 `version`과 배열 `pages`를 가져야 한다. 아니면 400 `"필기 데이터 형식이 올바르지 않습니다."`

- [ ] **Step 1: 실패하는 테스트 추가** — `create_contentNotObject_returns400`(`"123"`), `create_contentWithoutPages_returns400`(`{"version":1}`), `create_contentWithoutVersion_returns400`(`{"pages":[]}`), `create_contentWithPageSize_isAccepted`(`{"version":1,"page":{"width":1000,"height":1414},"pages":[{"strokes":[]}]}` → 201). 앞의 셋은 메시지 `"필기 데이터 형식이 올바르지 않습니다."`.
- [ ] **Step 2: 실패 확인** — Run: `.\gradlew.bat test --tests "*NoteApiTest"` → Expected: 새 테스트 3개 FAIL(201 반환)
- [ ] **Step 3: `validateContent` 수정** — `readTree` 결과가 객체이고 `version`이 숫자, `pages`가 배열인지 확인.
- [ ] **Step 4: 통과 확인** — Run: `.\gradlew.bat test` → Expected: 전체 PASS
- [ ] **Step 5: Commit** `feat: validate note content shape (version, pages)`

---

### Task 2: 백엔드 — OCR 호출 API (가짜 응답)

**Files:**
- Create: `src/main/java/kr/study/ddobak/ocr/OcrController.java`, `OcrClient.java`, `FakeOcrClient.java`, `OcrLineRequest.java`, `OcrResult.java`, `OcrChar.java`, `InvalidImageException.java`
- Modify: `src/main/java/kr/study/ddobak/common/GlobalExceptionHandler.java`
- Test: `src/test/java/kr/study/ddobak/ocr/OcrApiTest.java`

**Interfaces:**
- Produces:
  - `POST /api/ocr/line` 요청 `record OcrLineRequest(@NotBlank String imageBase64, @Min(1) @Max(4096) int width, @Min(1) @Max(4096) int height)` → 200 `OcrResult`
  - `record OcrResult(String engine, String text, List<OcrChar> chars)`
  - `record OcrChar(String ch, double x, double y, double w, double h, double confidence)` (줄 이미지 픽셀 좌표)
  - `interface OcrClient { OcrResult recognize(byte[] png, int width, int height); }` — 3주차에 Google 구현으로 교체
  - `FakeOcrClient`(@Component): `new OcrResult("fake", "", List.of())`
  - `InvalidImageException(String message)` → 400

- [ ] **Step 1: 실패하는 테스트** — `recognize_validPng_returnsFakeResult`(1×1 PNG base64 → 200, `$.engine`="fake", `$.chars` 빈 배열), `recognize_notBase64_returns400`(`"@@@"` → `"이미지 형식이 올바르지 않습니다."`), `recognize_notPng_returns400`(`"aGVsbG8="` → 같은 메시지), `recognize_tooLarge_returns400`(2MB 초과 → `"이미지가 너무 큽니다."`), `recognize_zeroWidth_returns400`(width 0 → 400, `$.message` 존재)
- [ ] **Step 2: 실패 확인** — Run: `.\gradlew.bat test --tests "*OcrApiTest"` → Expected: 컴파일 에러(클래스 없음)
- [ ] **Step 3: 구현** — 컨트롤러가 Base64 디코드(실패 시 `InvalidImageException("이미지 형식이 올바르지 않습니다.")`), 크기 2MB(2,097,152바이트) 초과 시 `"이미지가 너무 큽니다."`, PNG 시그니처(`89 50 4E 47 0D 0A 1A 0A`) 확인 후 `OcrClient` 호출. 예외 처리기에 `InvalidImageException` → 400 추가.
- [ ] **Step 4: 통과 확인** — Run: `.\gradlew.bat test` → Expected: 전체 PASS
- [ ] **Step 5: Commit** `feat: add OCR line API with fake client`

---

### Task 3: 프론트 — 필기 데이터와 좌표 함수

**Files:**
- Create: `src/note/content.js`, `src/note/content.test.js`, `src/note/geometry.js`, `src/note/geometry.test.js`
- Modify: `src/api/notes.js` (`EMPTY_CONTENT`를 `content.js`에서 다시 내보내기)

**Interfaces:**
- Produces (`content.js`):
  - `PAGE_WIDTH = 1000`, `PAGE_HEIGHT = 1414`
  - `EMPTY_CONTENT` (문자열, `page` 포함)
  - `parseContent(text) → {version, page, pages}` — `page` 없으면 기본값, `pages` 비면 빈 페이지 1개. JSON이 아니면 `Error('필기 데이터를 읽을 수 없습니다.')`
  - `serializeContent(content) → string`
  - `createStroke({ t0, color, width }) → Stroke` (id `s_${t0}_${n}`, n은 모듈 내 증가값)
  - `addPoint(stroke, { x, y, pressure, t }) → Stroke` (새 객체 반환, 반올림 규칙은 Global Constraints)
- Produces (`geometry.js`):
  - `applyTransform([x, y], transform) → [x, y]`
  - `strokePoints(stroke) → [[x, y, p]...]` (transform 적용)
  - `strokeBBox(stroke) → {minX, minY, maxX, maxY}` (transform 적용)
  - `rawBBox(stroke)` (transform 무시, 원본 기준)
  - `clientToPage(clientX, clientY, rect) → {x, y}`

- [ ] **Step 1: 실패하는 테스트** — 대표 단언:
```js
expect(parseContent('{"version":1,"pages":[{"strokes":[]}]}').page).toEqual({ width: 1000, height: 1414 })
expect(() => parseContent('hello')).toThrow('필기 데이터를 읽을 수 없습니다.')
const s = addPoint(createStroke({ t0: 1000, color: '#222222', width: 2.4 }), { x: 12.345, y: 6.789, pressure: 0, t: 1016.4 })
expect(s.points).toEqual([[12.3, 6.8, 0.5, 16]])
expect(applyTransform([10, 0], { dx: 5, dy: 0, rotate: Math.PI / 2, ox: 0, oy: 0 })).toEqual([5, 10]) // toBeCloseTo로 각 성분
expect(clientToPage(150, 100, { left: 100, top: 50, width: 500, height: 707 })).toEqual({ x: 100, y: 100 })
```
- [ ] **Step 2: 실패 확인** — Run: `node scripts/mini-vitest.mjs src/note` (Claude 쪽) → Expected: 모듈 없음 FAIL
- [ ] **Step 3: 구현**
- [ ] **Step 4: 통과 확인** — 같은 명령 → Expected: PASS
- [ ] **Step 5: Commit** `feat: add note content model and geometry helpers`

---

### Task 4: 프론트 — 줄 묶기와 글씨/그림 판정

**Files:**
- Create: `src/note/lines.js`, `src/note/lines.test.js`

**Interfaces:**
- Consumes: `strokeBBox`, `rawBBox` (Task 3)
- Produces:
  - 상수 `LINE_IDLE_MS = 2500`, `MIN_VERTICAL_OVERLAP = 0.3`, `MAX_GAP_RATIO = 2.0`, `DRAWING_HEIGHT_RATIO = 2.5`, `DEFAULT_TEXT_HEIGHT = 40`, `LONG_STROKE_ASPECT = 8`
  - `joinsGroup(stroke, group) → boolean` — 세로 겹침 비율(겹친 높이 ÷ 둘 중 작은 높이) ≥ 0.3 이고, 획 왼쪽 끝이 그룹 오른쪽 끝에서 `MAX_GAP_RATIO × 그룹 높이` 이내이며, 그룹 왼쪽 끝보다 그룹 높이 이상 왼쪽으로 가지 않을 것
  - `groupLines(strokes) → Line[]` — `t0` 순서로 처리, 들어갈 수 있는 그룹 중 세로 겹침이 가장 큰 그룹에 넣고 없으면 새 그룹. 결과는 `minY` 순 정렬. `Line = {id(첫 획 id), kind: 'text'|'drawing', strokeIds, bbox}`
  - 판정: 기준 높이 = 높이가 `3 × DEFAULT_TEXT_HEIGHT` 이하인 그룹들의 높이 중앙값(없으면 40). 그룹 높이 > `2.5 × 기준`이면 drawing. 획 1개이고 가로/세로 > 8 이고 가로 > `3 × 기준`이면 drawing(밑줄·화살표). 나머지 text
  - `isLineEnd({ line, nextStroke, idleMs }) → boolean` — `idleMs ≥ LINE_IDLE_MS` 이거나 `nextStroke`가 `line`에 `joinsGroup` 하지 않으면 true

- [ ] **Step 1: 실패하는 테스트** — 테스트용 `fakeStroke(id, t0, minX, minY, maxX, maxY)` 헬퍼(대각선 두 점)로:
  - `groups strokes written left to right on one line` — 높이 40, 간격 10인 획 5개 → text 줄 1개, strokeIds 5개
  - `starts a new line when writing moves below` — 두 번째 줄(y +70) → text 줄 2개, `minY` 순
  - `allows small backtracking within a line (받침)` — 앞 글자 아래쪽에 겹쳐 쓴 획 → 같은 줄
  - `classifies a tall group as drawing` — 높이 200 동그라미 → kind `drawing`, 옆 글씨 줄은 `text`
  - `classifies a long thin single stroke as drawing` — 가로 400, 세로 6 밑줄 → `drawing`
  - `adds a later stroke back into an earlier line` — 둘째 줄을 쓴 뒤 첫째 줄 끝에 이어 쓴 획 → 첫째 줄에 합류
  - `isLineEnd true after idle`, `isLineEnd true when next stroke starts below-left`, `isLineEnd false for next letter on the same line`
- [ ] **Step 2: 실패 확인** — `node scripts/mini-vitest.mjs src/note/lines.test.js` → FAIL
- [ ] **Step 3: 구현**
- [ ] **Step 4: 통과 확인** — 같은 명령 → PASS
- [ ] **Step 5: Commit** `feat: group strokes into lines and classify text vs drawing`

---

### Task 5: 프론트 — 노트 정리 (글씨 줄 반듯하게)

**Files:**
- Create: `src/note/tidy.js`, `src/note/tidy.test.js`

**Interfaces:**
- Consumes: `groupLines` (Task 4), `rawBBox` (Task 3)
- Produces:
  - 상수 `MAX_TIDY_ANGLE = 15°`(라디안으로 저장), `LEFT_SNAP = 40`, `PARAGRAPH_GAP_RATIO = 1.5`
  - `tidyPage(strokes) → Stroke[]` — 원본 좌표 기준으로 매번 새로 계산(이전 transform 무시 → 두 번 실행해도 같음). text 줄만:
    1. 기울기: 줄 안 각 획의 (가로 중심, 아래끝)으로 최소제곱 기울기 → 각도. |각도| > 15°면 0
    2. 회전: 줄 왼쪽 아래(기준선 시작점) 중심으로 −각도
    3. 왼쪽 맞춤: text 줄 왼쪽 끝들의 중앙값에서 `LEFT_SNAP` 이내인 줄은 중앙값으로 이동
    4. 줄 간격: 그림 없이 이어지는 text 줄 묶음(위아래 간격 < `1.5 × 기준 높이`) 안에서 간격을 그 묶음의 간격 중앙값으로 통일. 첫 줄은 그대로
  - drawing 줄의 획은 `transform: null`
  - `untidyPage(strokes) → Stroke[]` — 모든 `transform`을 `null`로

- [ ] **Step 1: 실패하는 테스트**
  - `levels a tilted line` — 오른쪽으로 갈수록 5° 올라가는 글씨 줄 → 정리 후 획 아래끝 기울기 |m| < 0.01
  - `does not rotate a line tilted more than 15 degrees` — 30° → rotate 0
  - `leaves drawings untouched` — 그림 획의 transform은 `null`, `points`는 입력과 동일
  - `never changes original points` — 모든 획의 `points`가 입력과 깊은 비교로 같음
  - `is idempotent` — `tidyPage(tidyPage(s))`의 transform == `tidyPage(s)`의 transform
  - `snaps nearly aligned left margins` — 왼쪽 끝 100, 112, 95인 세 줄 → 정리 후 세 줄 왼쪽 끝 차이 < 0.5
  - `evens line spacing within a paragraph` — 간격 60, 80인 세 줄 → 정리 후 두 간격 차이 < 0.5
  - `does not pull lines across a drawing` — 줄, 그림, 줄 순서면 그림 아래 줄의 세로 이동량 0
  - `untidyPage clears transforms`
- [ ] **Step 2: 실패 확인** — `node scripts/mini-vitest.mjs src/note/tidy.test.js` → FAIL
- [ ] **Step 3: 구현**
- [ ] **Step 4: 통과 확인** → PASS
- [ ] **Step 5: Commit** `feat: tidy text lines without touching drawings`

---

### Task 6: 프론트 — 캔버스, 에디터, 자동 저장

**Files:**
- Create: `src/note/render.js`, `src/components/NoteCanvas.jsx`, `src/components/NoteEditor.jsx`, `src/components/NoteEditor.css`
- Modify: `src/App.jsx` (목록 ↔ 에디터 전환), `src/App.css`

**Interfaces:**
- Consumes: Task 3~5 전부, `getNote`, `updateNote` (1주차)
- Produces:
  - `drawPage(ctx, { strokes, scale, showGuides, lines, showLines })` — 가이드 줄(간격 50, 연한 회색), 획(굵기 = `width × (0.4 + 1.2 × pressure) × scale`, 둥근 끝), `showLines`면 text 줄 파란 테두리·drawing 빨간 테두리(기준값 튜닝용)
  - `<NoteCanvas strokes onStrokesChange tool color showGuides showLines onStrokeStart onStrokeEnd />` — 펜·마우스만 그림, 손가락은 세로 스크롤, `getCoalescedEvents()`로 점 보강, 지우개는 닿은 획 통째 삭제(반경 10)
  - `<NoteEditor noteId onBack />` — 툴바(목록, 펜/지우개, 색 3개 #222222/#1e5bd8/#d32f2f, 실행 취소/다시 실행(최대 50), 가이드 줄, 줄 보기, 노트 정리/정리 취소), 제목 수정, 마지막 변경 1초 뒤 자동 저장, "저장 중/저장됨/저장 실패" 표시

- [ ] **Step 1: 구현** (UI라 순수 함수 테스트 없음)
- [ ] **Step 2: 확인** — 사용자 PC: `npm test`, `npm run lint` → Expected: PASS, 오류 없음
- [ ] **Step 3: PC 수동 확인** — 마우스로 쓰기, 지우개, 실행 취소, 새로고침 후 그대로
- [ ] **Step 4: iPad 수동 확인** — Pencil 필압으로 굵기 변화, 손바닥 대고 써도 손바닥 자국 없음, 손가락 스크롤, 1주차에 만든 노트 열기
- [ ] **Step 5: Commit** `feat: add note editor with pencil canvas and autosave`

---

### Task 7: 프론트 — 한 줄 끝 → OCR 호출, 노트 정리 버튼 연결

**Files:**
- Create: `src/api/ocr.js`, `src/api/ocr.test.js`, `src/note/lineImage.js`
- Modify: `src/components/NoteEditor.jsx`

**Interfaces:**
- Consumes: `isLineEnd`, `groupLines` (Task 4), `tidyPage`/`untidyPage` (Task 5), `POST /api/ocr/line` (Task 2)
- Produces:
  - `recognizeLine({ imageBase64, width, height }) → Promise<OcrResult>` (오류 시 서버 `message`로 Error)
  - `renderLineToPng(strokes, bbox, { padding: 12, scale: 2 }) → { imageBase64, width, height }` (흰 배경 PNG, `data:` 접두어 없음)
  - 에디터: 펜을 뗀 뒤 `LINE_IDLE_MS` 경과 또는 다음 획이 새 줄을 시작하면, 끝난 text 줄 하나에 대해 `recognizeLine` 호출(같은 줄 중복 호출 방지). 결과는 메모리에만 두고 "줄 보기"일 때 줄 옆에 `fake` 표시

- [ ] **Step 1: 실패하는 테스트** (`ocr.test.js`) — `posts to /api/ocr/line and returns result`, `throws server message on 400`
- [ ] **Step 2: 실패 확인** → `node scripts/mini-vitest.mjs src/api/ocr.test.js` → FAIL
- [ ] **Step 3: 구현**
- [ ] **Step 4: 통과 확인** → PASS, 사용자 PC `npm test` PASS
- [ ] **Step 5: iPad 수동 확인** — 한 줄 쓰고 2.5초 뒤 "줄 보기"에 `fake` 표시, 그림에는 표시 없음, "노트 정리" → 기울어진 줄이 반듯해지고 "정리 취소"로 원래대로
- [ ] **Step 6: Commit** `feat: call OCR when a line ends and wire tidy button`

---

### Task 8: PWA — 홈 화면에 추가하면 앱처럼

**Files:**
- Create: `public/manifest.webmanifest`, `public/icons/icon-192.png`, `public/icons/icon-512.png`, `public/icons/apple-touch-icon.png`(180)
- Modify: `index.html`, `src/index.css`

**Interfaces:**
- Produces: 이름·짧은 이름 "또박", `display: standalone`, `start_url: "/"`, 흰 배경. `index.html`: `lang="ko"`, `<title>또박</title>`, `apple-mobile-web-app-capable`, `apple-mobile-web-app-title`, `apple-touch-icon`, `viewport-fit=cover`. CSS: `touch-action: manipulation`(더블탭 확대 방지), 캔버스 영역 `user-select: none`, `-webkit-touch-callout: none`, `overscroll-behavior: none`

- [ ] **Step 1: 구현** (아이콘은 Claude가 생성)
- [ ] **Step 2: iPad 수동 확인** — 공유 → 홈 화면에 추가 → 아이콘 "또박", 주소창 없이 열림, 필기 중 확대·텍스트 선택 안 됨
- [ ] **Step 3: Commit** `feat: make the app installable to the iPad home screen`

**알려진 제약:** Cloudflare 임시 터널은 실행할 때마다 주소가 바뀌어, 홈 화면 아이콘이 다음 실행 때 열리지 않는다. 고정 주소 방식(Tailscale 또는 ngrok 고정 도메인)은 사용자 결정 후 별도 작업.
