# 연습 모드 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 고른 글씨체를 칸에 따라 쓰면 앱이 점수(모양·위치/크기·기울기)를 계산하고, 서버가 기록을 저장하며 Gemini가 조언 1~2문장을 붙인다.

**Architecture:** 점수는 프론트의 순수 함수(`score.js`)가 64×64 모눈 두 장을 받아 계산하고, 캔버스 의존은 `raster.js`에만 둔다. 백엔드는 `practice` 패키지에 기록 CRUD와 `AdviceClient` 인터페이스(Gemini/Fake)를 두고, 기록을 먼저 저장한 뒤 조언을 붙인다.

**Tech Stack:** React 19 + Vite 8 + Vitest / Spring Boot 4.1.2 (Jackson 3 `tools.jackson`), Java 21, JPA, MySQL + Flyway, 테스트 H2(MySQL 모드). 새 의존성 없음.

**Spec:** `docs/superpowers/specs/2026-11-13-practice-mode-design.md` (백엔드 저장소)

## Global Constraints

- 새 라이브러리 추가 금지(프론트·백엔드 모두). Gemini는 `RestClient`로 직접 호출
- 키·비밀번호는 커밋 금지. Gemini 키는 `secret.yaml`의 `ddobak.advice.gemini.api-key`(환경 변수 `GEMINI_API_KEY`), 비면 `FakeAdviceClient`
- V1 SQL은 고치지 않는다. 새 표는 `V2__create_practice_attempt.sql`, 테스트 yaml `schema-locations`에도 추가. `ddl-auto: validate` 유지
- 모든 `/api/practice/**`는 로그인 필요(`@LoginUser Long userId`), 남의 기록은 404
- 사용자에게 보이는 글·오류 메시지는 한국어 해요체. 조언 실패 문구는 정확히 `지금은 조언을 할 수 없어요`
- 획 형식은 노트와 같다: `{ id, points: [[x, y, pressure, ms], ...] }`. 연습 획은 칸 좌표(0~100) 기준
- 글씨체는 "fontId → `fontFamily(fontId)`"만 통해 그린다(고정 5종 목록에 채점·가이드·조언 그림을 묶지 않음)
- 채점 기준값은 `SCORE_TUNING` 하나에: `GRID 64, NEAR 3, F_MIN 0.30, F_MAX 0.95, CENTER_MAX 0.25, SIZE_MAX 1.6, TILT_FREE_DEG 3, TILT_MAX_DEG 20, SHEAR_RANGE 0.5, SHEAR_STEP 0.05, PEN 3, GLYPH_FILL 0.8`
- 커밋 작성자 `Junyeong0721`, 메시지 끝에 Co-Authored-By / Claude-Session 줄
- 브랜치: 백엔드 `feat/practice`(지금 `docs/practice-design` 위에서), 프론트 `feat/practice`
- 검증 도구(이 작업 환경): 프론트 `/home/claude/tools/mv src`, 백엔드 `/home/claude/tools/jtest <dir> [TestClass]`, 화면 Playwright `/home/claude/smoke/`

## Review Focus

1. **칸 밖·점 하나짜리 획**: 점 1개 획, 칸 밖으로 크게 나간 획 → 예외 없이 채점(잉크 0이면 0점 "안 씀"). → Task 2 테스트
2. **목표 글자가 글꼴에 없음**(직접 입력에 영문·숫자·이모지): 목표 잉크가 0인 칸은 채점 대상에서 빼고 세트 평균에도 넣지 않는다, 전부 빠지면 [채점하기] 비활성. → Task 2·6 테스트
3. **글꼴이 아직 안 받아졌을 때 채점**: `ensureFont` 완료 전에는 채점·가이드를 그리지 않는다(기본 글꼴로 채점되면 점수가 틀림). → Task 3 테스트
4. **조언 중 화면 이탈·중복 누름**: [채점하기] 연타 시 기록이 두 번 저장되지 않는다, 응답이 오기 전에 나가도 오류 없음. → Task 6 테스트
5. **Gemini가 느리거나 이상한 답**(빈 글, 300자 초과, 4xx/5xx, 10초 초과): 모두 `UNAVAILABLE`, 기록은 저장됨, 실패한 호출은 하루 횟수에서 빼지 않는다. → Task 5 테스트

---

## 파일 구조

**백엔드** `src/main/java/kr/study/ddobak/practice/`
- `PracticeAttempt.java`, `PracticeAttemptRepository.java`, `PracticeService.java`, `PracticeController.java`
- `PracticeAttemptRequest.java`, `PracticeAttemptResponse.java`, `PracticeAttemptSummary.java`, `PracticeBest.java`, `PracticeNotFoundException.java`
- `advice/AdviceClient.java`, `AdviceRequest.java`, `GeminiAdviceClient.java`, `FakeAdviceClient.java`, `AdviceConfig.java`, `AdviceUsageLimiter.java`, `AdviceFailedException.java`
- `src/main/resources/db/migration/V2__create_practice_attempt.sql`

**프론트** 
- `src/practice/`: `sets.js`, `cells.js`, `score.js`, `raster.js`, `adviceImage.js`, `practiceFont.js`
- `src/api/practice.js`
- `src/components/practice/`: `Practice.jsx`(세 화면 전환), `PracticeHome.jsx`, `PracticeBoard.jsx`, `PracticeResult.jsx`, `FontPicker.jsx`, `Practice.css`
- 수정: `src/App.jsx`, `src/components/Home.jsx`, `src/components/Landing.jsx`

---

### Task 1: 세트와 칸 배치 (프론트)

**Files:** Create `src/practice/sets.js`, `src/practice/cells.js`, `src/practice/practiceFont.js` + 각 `.test.js`

**Interfaces — Produces:**
- `STAGES: [{ id: 'jamo'|'syllable'|'word'|'sentence', label }]`, `SETS: [{ id, stage, text }]`(spec 3장 표 16개 그대로), `setsOf(stageId)`, `nextSet(setId) → set | null`(같은 단계 다음, 마지막이면 null)
- `MAX_TEXT = 30`, `COLS = 10`, `CELL = 100`
- `normalizeText(raw) → string`: 앞뒤 공백 제거, 연속 공백 하나로, 30자로 자름
- `layoutCells(text) → [{ index, ch, row, col, x, y, blank }]`: 한 줄 10칸, `x = col*100`, `y = row*100`, 공백은 `blank: true`
- `cellAt(cells, px, py) → index | -1`(blank 칸은 -1)
- `toCellStroke(stroke, cell) → stroke`(점 좌표에서 `cell.x, cell.y`를 뺌)
- `loadPracticeFont() → fontId`(localStorage `ddobak.practice.fontId`, 없으면 `loadSettings().fontId`), `savePracticeFont(fontId)`

- [ ] **Step 1: 실패하는 테스트** — `sets.test.js`: 세트 16개, id 유일, 모든 text가 `normalizeText(text)`와 같고 30자 이하, `nextSet('consonant-1').id === 'consonant-2'`, `nextSet('vowel-2') === null`. `cells.test.js`: `layoutCells('가나 다')` → 4칸·3번째 `blank`, 11자면 11번째가 `row 1, col 0`, `cellAt`이 blank 칸·범위 밖에서 -1, `normalizeText('  가  나 ') === '가 나'`, 31자 → 30자. `practiceFont.test.js`: 저장값 없으면 설정 글씨체, 저장 후 그 값, 설정은 안 바뀜
- [ ] **Step 2:** `/home/claude/tools/mv src` → 새 테스트 FAIL 확인
- [ ] **Step 3:** 구현
- [ ] **Step 4:** `/home/claude/tools/mv src` → 전부 PASS
- [ ] **Step 5:** Commit `feat: add practice sets and cell layout`

### Task 2: 점수 계산 (프론트, 순수 함수)

**Files:** Create `src/practice/score.js`, `src/practice/score.test.js`

**Interfaces — Produces:**
- `SCORE_TUNING`(Global Constraints 값)
- 모눈 `Grid = { w, h, data: Uint8Array }`(1 = 잉크)
- `scoreGlyph(target: Grid, mine: Grid) → { total, shape, position, tilt, empty, skipped }` — `shape` 0~50, `position` 0~25, `tilt` 0~25 (소수), `total` 반올림 정수. `mine` 잉크 0 → 전부 0, `empty: true`. `target` 잉크 0 → `skipped: true`
- `scoreSet(glyphs: Array<glyph|null>) → { total, shapePct, positionPct, tiltPct, counted }` — `null`(공백)과 `skipped`는 제외, `empty`는 0점으로 포함, 각 Pct는 만점 대비 0~100 정수
- 내부(테스트용 export): `bbox(grid)`, `fitTo(mine, target)`, `shear(grid, s)`, `distanceMap(grid)`, `fMeasure(a, b, near)`

**알고리즘(spec 4.2~4.4 그대로):** ① `fitTo`: 상자 중심 맞춤 + `min(tw/mw, th/mh)` 같은 비율 확대(최근접 표본) ② s를 −0.5~0.5, 0.05 간격으로 `shear(fitted, s)` 후 `fMeasure` 최대인 s 선택(동점이면 |s| 작은 쪽) ③ `fMeasure`: 2-pass chamfer(3-4 가중치/3) 거리 지도, P·R은 거리 ≤ 3 비율, F = 2PR/(P+R) ④ `shape = 50·clamp((F−0.30)/0.65)` ⑤ `position = 25·(0.5·clamp(1 − d/0.25) + 0.5·clamp(1 − |ln r|/ln 1.6))`, d·r은 맞추기 전 상자 기준 ⑥ `tilt = 25·clamp(1 − (deg(|atan s|) − 3)/17)`

- [ ] **Step 1: 실패하는 테스트** — 헬퍼 `gridFrom(drawFn)`으로 64×64에 굵기 3 선분을 그려 'ㅏ' 모양(세로선 + 가로 짧은 선), '가' 비슷한 모양을 만든다.
  - 같은 모눈 → `total >= 95`
  - 오른쪽으로 12모눈 밀기 → `shape >= 45`, `tilt >= 24`, `position < 20`
  - 2배 확대(중심 유지) → `shape >= 40`, `position <= 13`
  - `shear(target, 0.35)` → `tilt < 10`, `shape >= 40`, `position >= 20`
  - 가로 획 제거 → `shape < 35`
  - 무작위 낙서(고정 시드 좌표) → `total < 40`
  - 빈 `mine` → `total 0, empty true`; 빈 `target` → `skipped true`
  - 잉크 1모눈짜리 `mine` → 예외 없이 숫자 반환, 0~100
  - 같은 입력 두 번 → 깊은 동등
  - `scoreSet([g90, null, gEmpty, gSkipped])` → `counted 2`, `total 45`
- [ ] **Step 2:** FAIL 확인
- [ ] **Step 3:** 구현
- [ ] **Step 4:** PASS. 기준을 못 맞추는 테스트가 있으면 테스트 임계값이 아니라 알고리즘을 먼저 의심하고, 그래도 안 되면 `SCORE_TUNING`을 바꾸고 spec 4장 숫자도 같이 고친다
- [ ] **Step 5:** Commit `feat: score practice glyphs by shape, position and tilt`

### Task 3: 모눈 만들기와 조언 그림 (프론트, 캔버스)

**Files:** Create `src/practice/raster.js`, `src/practice/adviceImage.js`, `src/practice/raster.test.js`

**Interfaces — Consumes:** `fontFamily`, `ensureFont`(`src/note/fonts.js`), `SCORE_TUNING`, `layoutCells`
**Produces:**
- `glyphPx(fontId, cellPx) → px`: 글꼴의 '가' 실제 높이가 `cellPx·0.8`이 되는 font-size(결과 캐시). 노트 보정(`render.js`)의 측정 함수가 export 가능하면 그것을 재사용하고 중복 구현하지 않는다
- `drawTarget(ctx, ch, fontId, x, y, cellPx, { alpha = 1, color })`: 칸 가운데에 글자를 그림(가이드·결과·조언 그림 공용)
- `drawCellStrokes(ctx, strokes, x, y, cellPx, { color, width })`: 칸 좌표(0~100) 획을 그림
- `targetGrid(ch, fontId) → Promise<Grid>`: `ensureFont` 대기 후 64×64 캔버스에 그려 알파 > 127을 잉크로
- `strokesGrid(strokes) → Grid`: 굵기 `PEN`(3모눈), 필압 무시, 점 1개 획은 점으로
- `gridFromImageData(imageData) → Grid`(순수, 테스트 대상)
- `renderAdviceImage({ text, fontId, cellStrokes }) → Promise<dataUrl>`: 칸 96px 흰 배경, 왼쪽 여백에 "목표"/"내 글씨" 라벨, 10칸마다 목표 줄·내 글씨 줄 쌍 반복, `image/png`

- [ ] **Step 1: 실패하는 테스트** — `gridFromImageData`: 4×4 RGBA에서 알파 128 → 1, 127 → 0. `strokesGrid`: (10,50)→(90,50) 획 → 가운데 줄 잉크, 위쪽 줄 비어 있음; 점 1개 획 → 잉크 ≥ 1; 칸 밖(-50, 200) 점이 섞여도 예외 없음. (캔버스가 없는 테스트 환경이면 `strokesGrid`는 캔버스 없이 선분을 직접 모눈에 찍는 방식으로 구현 → 테스트 가능)
- [ ] **Step 2:** FAIL 확인
- [ ] **Step 3:** 구현. `targetGrid`·`renderAdviceImage`·`drawTarget`은 캔버스가 필요하므로 Task 7의 Playwright에서 검증
- [ ] **Step 4:** PASS
- [ ] **Step 5:** Commit `feat: rasterize practice glyphs and build the advice image`

### Task 4: 연습 기록 저장·조회 (백엔드)

**Files:** Create `V2__create_practice_attempt.sql`, `practice/` 엔티티·리포지토리·서비스·컨트롤러·DTO, `PracticeNotFoundException`; Modify `GlobalExceptionHandler.java`(404), `src/test/resources/application.yaml`(schema-locations에 V2 추가, 쉼표 구분); Test `practice/PracticeApiTest.java`, `practice/PracticeSchemaTest.java`

**Interfaces — Produces:**
- 표: spec 6.1 SQL 그대로(`target_text`, 점수 INT, `strokes LONGTEXT`, `char_scores TEXT`, `advice VARCHAR(300) NULL`)
- `POST /api/practice/attempts` 요청 `{ setId?, text, fontId, guide, cells: [{ ch, strokes: [...] }|null], charScores: [{ total, shape, position, tilt, empty }|null], totalScore, shapeScore, positionScore, tiltScore, image }` → 201 `{ id, setId, text, fontId, guide, cells, charScores, totalScore, shapeScore, positionScore, tiltScore, advice, adviceStatus, createdAt }`. 이 Task에서는 `advice: null, adviceStatus: "UNAVAILABLE"` 고정(Task 5에서 연결)
- `GET /api/practice/attempts` → 최근 20개 `[{ id, setId, text, fontId, totalScore, createdAt }]`(최신순)
- `GET /api/practice/attempts/{id}` → 위 201 응답과 같은 모양(`adviceStatus`는 advice 있으면 `OK`)
- `GET /api/practice/best` → `[{ text, best, count, lastAt }]`
- `PracticeService.save(Long userId, PracticeAttemptRequest) → PracticeAttempt`, `attachAdvice(Long attemptId, String advice)`
- `cells`·`charScores`는 서버에서 JSON 문자열로 저장(`JsonMapper`), 응답에서는 다시 JSON으로

**검증(400, 한국어 메시지):** text 1~30자(공백만이면 400), fontId 1~40자, `cells.size() == text 글자 수`(코드포인트 기준), `charScores.size()`도 같음, 네 점수 0~100, 직렬화한 cells 2MB 이하, image는 `data:image/png;base64,`로 시작하고 1MB 이하(없어도 됨 → 조언만 생략)

- [ ] **Step 1: 실패하는 테스트** (`TestAuth`로 로그인한 MockMvc, 기존 `NoteApiTest` 방식)
  - 저장 201 → 응답에 id·text·totalScore, `adviceStatus`가 있음
  - 저장 후 `GET /attempts`에 보이고 획(cells)은 목록에 없음, `GET /attempts/{id}`에는 cells 포함
  - 21개 저장 → 목록 20개, 최신순
  - 같은 text로 60점·85점 저장 → `/best`에 `best 85, count 2`
  - 다른 계정으로 `GET /attempts/{id}` → 404, 다른 계정 `/best`·목록은 빈 배열
  - 로그인 없이 → 401
  - 400: text 빈 값, 31자, cells 개수 불일치, totalScore 101, image가 `data:image/jpeg`
  - 한글 text가 깨지지 않고 왕복
  - `PracticeSchemaTest`: 컨텍스트가 validate로 뜨고, V2 SQL에 `target_text`·`fk_practice_user`·`ON DELETE CASCADE` 포함, 회원 삭제 시 기록도 삭제
- [ ] **Step 2:** `/home/claude/tools/jtest . PracticeApiTest` → FAIL
- [ ] **Step 3:** 구현. `guide` BOOLEAN이 H2 validate에 걸리면 엔티티에 `columnDefinition`을 맞추는 대신 SQL을 `TINYINT(1)`/`BIT`로 바꾸지 말고 `@JdbcTypeCode`로 해결을 먼저 시도, 결과를 spec 6.1 메모에 반영
- [ ] **Step 4:** `/home/claude/tools/jtest .` 전체 PASS (기존 68개 포함)
- [ ] **Step 5:** MySQL DDL 확인: 앱을 `database-platform=MySQLDialect` + `schema-generation.scripts`로 띄워 뽑은 DDL과 V2 SQL의 타입이 맞는지 비교(기존 방식)
- [ ] **Step 6:** Commit `feat: save practice attempts per account`

### Task 5: Gemini 조언 (백엔드)

**Files:** Create `practice/advice/*`; Modify `PracticeService`/`PracticeController`(조언 연결), `application.yaml`, `secret.example.yaml`; Test `advice/GeminiAdviceClientTest.java`, `advice/AdviceUsageLimiterTest.java`, `advice/AdviceConfigTest.java`, `practice/PracticeAdviceApiTest.java`

**Interfaces — Produces:**
- `record AdviceRequest(String fontLabel, String text, boolean guide, List<CharScore> scores, byte[] pngImage)`
- `interface AdviceClient { String advise(AdviceRequest request); }` — 실패는 `AdviceFailedException`
- `GeminiAdviceClient(RestClient.Builder, JsonMapper, String apiKey, String model)`: `POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`, 헤더 `x-goog-api-key`, 본문 `contents[0].parts = [{ inline_data: { mime_type: "image/png", data: base64 } }, { text: 프롬프트 }]`, `generationConfig: { temperature: 0.4, maxOutputTokens: 200 }`. 응답 `candidates[0].content.parts[*].text`를 이어 붙이고 trim; 비었거나 300자 초과 → `AdviceFailedException`
- 프롬프트(고정 문구): 위 줄은 목표 글씨체(이름), 아래 줄은 학습자 손글씨 / 연습한 글 / 가이드 여부 / 글자별 점수(모양 50·위치와 크기 25·기울기 25 만점) / "점수가 가장 낮은 글자와 항목 위주로 고치는 방법을 한국어 해요체 1~2문장, 120자 이내로. 획순은 언급하지 말고, 점수를 다시 매기지 마세요."
- `FakeAdviceClient`: `"가짜 조언: 세로 획을 곧게 내려 보세요."`
- `AdviceConfig`: 키 비면 Fake, 있으면 Gemini(연결 5초, 읽기 10초). 로그는 OcrConfig와 같은 형식
- `AdviceUsageLimiter(int dailyLimit, Clock)`: `boolean tryAcquire(Long userId)`, `void release(Long userId)`(실패한 호출 되돌리기), 계정·Asia/Seoul 날짜별
- 설정: `ddobak.advice.daily-limit: ${ADVICE_DAILY_LIMIT:50}`, `ddobak.advice.gemini.api-key: ${GEMINI_API_KEY:}`, `ddobak.advice.gemini.model: ${GEMINI_MODEL:gemini-3.5-flash-lite}`(실제 키로 확인 후 조정)
- 요청에 fontLabel이 필요하므로 `PracticeAttemptRequest`에 `fontLabel`(선택, 40자 이하, 없으면 fontId) 추가
- 흐름: 컨트롤러가 `service.save()`(트랜잭션 끝) → image 없으면 UNAVAILABLE → `tryAcquire` 실패면 UNAVAILABLE → `advise` 성공 시 `attachAdvice` + `OK`, 예외 시 `release` + UNAVAILABLE(경고 로그, 키는 로그에 남기지 않음). 외부 호출은 DB 트랜잭션 밖

- [ ] **Step 1: 실패하는 테스트**
  - `GeminiAdviceClientTest`(MockRestServiceServer): 요청 URL에 모델, `x-goog-api-key` 헤더, 본문에 `inline_data.mime_type = image/png`·프롬프트에 연습한 글과 "획순" 문구; 정상 응답 → 조언 문자열; 빈 candidates·빈 text·301자·HTTP 429·500 → `AdviceFailedException`
  - `AdviceUsageLimiterTest`: 한도 2면 세 번째 false, 다른 계정은 별도, 날짜 바뀌면 초기화, `release` 후 다시 true
  - `AdviceConfigTest`: 키 없음 → Fake, 있음 → Gemini
  - `PracticeAdviceApiTest`(`@MockitoBean AdviceClient` 또는 테스트용 구현 주입 — 기존 OCR 테스트 방식 따름): 성공 → `advice` + `OK`, 다시 조회해도 advice 있음; 클라이언트 예외 → 201·`UNAVAILABLE`·기록은 저장됨·횟수 되돌려짐; image 없음 → 호출 안 함; 한도 1로 두 번 → 두 번째 `UNAVAILABLE`이고 클라이언트 1번만 호출
- [ ] **Step 2:** FAIL 확인
- [ ] **Step 3:** 구현
- [ ] **Step 4:** `/home/claude/tools/jtest .` 전체 PASS
- [ ] **Step 5:** `secret.example.yaml`에 Gemini 키 자리 추가, `docs/gemini-setup.md`(AI Studio에서 결제 계정 없는 새 프로젝트로 키 만들기, 한도 확인 위치) 작성
- [ ] **Step 6:** Commit `feat: add Gemini advice to practice attempts`

### Task 6: 연습 화면 (프론트)

**Files:** Create `src/api/practice.js`(+test), `src/components/practice/*`; Modify `src/App.jsx`(view `practice`), `Home.jsx`(카드 → `onPractice`, "곧 열려요" 제거), `Landing.jsx`(`soon` 제거)

**Interfaces — Consumes:** Task 1~3 전부, Task 4·5 API, `FONTS`/`fontById`/`ensureFont`, `request`(`src/api/notes.js`)
**Produces:** `saveAttempt(body)`, `listAttempts()`, `getAttempt(id)`, `listBest()`; `<Practice onExit />`가 내부 상태 `screen: 'home'|'board'|'result'`로 세 화면 전환

**동작(spec 2장):**
- `PracticeHome`: `FontPicker`(5종, 이름을 그 글씨체로), 단계 탭, 세트 카드(최고 점수는 `listBest`를 text로 맞춤, 없으면 "아직 안 함"), 직접 입력(`normalizeText`, 빈 값이면 [시작] 비활성), 최근 연습 20개 → `getAttempt` → 결과 화면(다시 보기)
- `PracticeBoard`: 캔버스 하나에 칸·열십자·가이드(`alpha 0.18`)·내 획. 가이드 토글 기본 켜짐. Pointer Events: `pen`이 한 번 들어오면 이후 `touch` 무시, `pen`이 없으면 `touch`·`mouse`로 쓴다. 획은 **시작점**의 칸에 속함(`cellAt`, blank·칸 밖 시작은 버림). 칸을 누르면 선택 → [칸 지우기], [전체 다시]. 글씨체 변경 시 획 유지. 글꼴 로딩 중에는 가이드 대신 칸만, [채점하기] 비활성. 쓴 칸이 0이거나 채점 가능한 칸이 0이면 비활성
- [채점하기]: 모든 칸 `targetGrid`/`strokesGrid` → `scoreGlyph` → `scoreSet` → 즉시 결과 화면. 동시에 `renderAdviceImage` → `saveAttempt`(한 번만: 진행 중 플래그), 응답의 `advice`/`adviceStatus`로 조언 자리 갱신. `saveAttempt` 자체가 실패하면 조언 자리에 `지금은 조언을 할 수 없어요`, 그 아래 작은 글씨 "기록을 저장하지 못했어요"
- `PracticeResult`: 칸마다 목표(회색 `#b9bfcc`) + 내 획(남색, 기존 잉크색 변수) 겹쳐 그림, 글자 점수("안 씀"/제외 칸은 "–"), 세트 점수, 막대 3개(모양/위치·크기/기울기 %), 조언 자리(`조언을 생각하는 중…` → 문장 / `지금은 조언을 할 수 없어요`), [다시 하기] [다음 세트](`nextSet`이 null이거나 직접 입력이면 숨김) [고르기로]. 화면을 떠난 뒤 응답이 와도 상태 갱신하지 않음
- 디자인: 기존 토큰(흰 종이·남색 잉크·노란 형광펜·인덱스 탭 색, IBM Plex Sans KR)과 `Home` 카드 스타일을 따른다. 칸은 iPad 가로에서 10칸이 한 줄에 들어가고 폰에서는 칸 크기를 줄여 가로 스크롤 없이 맞춘다

- [ ] **Step 1: 실패하는 테스트** — `practice.test.js`(기존 `notes.test.js` 방식): 네 함수의 경로·메서드·본문, 401이면 `ddobak:unauthorized`. 순수 로직은 컴포넌트 밖으로 빼서 테스트: `src/practice/board.js`에 `pickInput(prevSawPen, pointerType) → { accept, sawPen }`, `canGrade(cells, cellStrokes, fontReady) → boolean`, `buildAttemptBody({ set, text, fontId, guide, cells, cellStrokes, glyphScores, setScore, image })` → Task 4 요청 모양. 테스트: 펜 뒤 터치 무시·펜 없이 터치 허용, 빈 칸뿐이면 false·글꼴 준비 전 false, body의 `cells.length === [...text].length`·blank 칸은 null
- [ ] **Step 2:** FAIL 확인
- [ ] **Step 3:** 구현
- [ ] **Step 4:** `/home/claude/tools/mv src` 전체 PASS, `npx oxlint`(가능하면) 경고 없음, esbuild 번들(`--external:/fonts/*`) 성공
- [ ] **Step 5:** Commit `feat: add the practice screens`

### Task 7: 화면 흐름 확인, 문서, PR

**Files:** `/home/claude/smoke/server3.mjs`(가짜 `/api/practice/*` 추가: 메모리에 저장, `practice.advice: 'ok'|'fail'|'slow'` 옵션), `/home/claude/smoke/practice.mjs`; 백엔드 `docs/dev-log/2026-11-14-practice-mode.md`

- [ ] **Step 1:** Playwright 시나리오(iPad 크기 + 폰 크기 각각)
  1. 로그인 → 홈 "글씨 연습" → 고르기 화면, 세트 16개가 단계별로 보임
  2. `consonant-1` 선택 → 가이드 보임, 가이드 토글 끄면 사라짐
  3. 가상 펜으로 각 칸의 가이드 위치를 따라 획 입력(목표 글자 모눈의 잉크 중심선을 따라가는 좌표를 페이지에서 계산) → [채점하기] → 1초 안에 결과, 세트 점수 ≥ 70
  4. 엉뚱한 낙서 입력 → 세트 점수 < 40
  5. 조언: `ok` → 문장 표시, `fail` → `지금은 조언을 할 수 없어요`, `slow` 중 [채점하기] 연타 → 저장 요청 1번
  6. [다음 세트] → `consonant-2`, [고르기로] → 최고 점수·횟수 반영, 최근 연습에서 다시 보기 → 같은 점수·조언
  7. 글씨체를 하이멜로디로 바꾸면 가이드 모양이 바뀌고 설정 글씨체(localStorage `ddobak.settings`)는 그대로
  8. 직접 입력 `가나 abc` → a·b·c 칸은 "–"로 제외, 평균은 가·나만
  9. 스크린샷을 찍어 디자인 확인(칸 넘침·겹침·폰 가로 스크롤 없음)
- [ ] **Step 2:** 실패 항목 수정 후 프론트·백엔드 전체 테스트 재실행
- [ ] **Step 3:** 개발 기록 작성, spec에서 구현 중 바뀐 값 반영
- [ ] **Step 4:** 두 저장소 push, PR 생성(`gh api`), PC로 bundle 전달(브랜치 받아 두기)
- [ ] **Step 5:** 사용자 안내: Gemini 키 만들기(`docs/gemini-setup.md`), iPad에서 확인할 것, 머지 순서(백엔드·프론트 같이), 프로젝트 진행 상황 문서 갱신
