# 1주차: 노트 저장 API + React 연결 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 노트를 저장·조회·수정·삭제하는 Spring Boot REST API를 만들고, React(Vite) 앱이 PC와 iPad 양쪽에서 그 API를 호출하는 것까지 확인한다.

**Architecture:** 저장소 2개. 백엔드 `ddobak-backend-springboot`(GitHub `NOTE_DDOBAK_BACKEND`), 프론트 `ddobak-frontend-react`(GitHub `NOTE_DDOBAK_FRONTEND`). 이 계획과 개발 기록(`docs/`)은 백엔드 저장소에 둔다. 노트의 필기 데이터(`content`)는 프론트가 만든 JSON 문자열을 백엔드가 그대로 저장한다(백엔드는 JSON 형식인지만 검사). 개발 중에는 Vite 개발 서버가 `/api` 요청을 `localhost:8080`으로 프록시하므로 CORS 설정이 필요 없고, iPad에서도 같은 주소로 동작한다.

**Tech Stack:** Java 21, Spring Boot 4.1.2 (Gradle - Groovy), Spring Web, Spring Data JPA, Validation, Lombok, DevTools, H2(1주차 임시 DB), React 19 + Vite 8(JavaScript), Oxlint, Vitest

**Spec:** AI 손글씨 보정 노트 앱 기획서 v0.5 — https://claude.ai/artifact/37fZAQdu7t8TMxbCMRu1JV (3장 "노트 기본 기능 (MVP)", 6장 "기술 스택", 8장 "4주 일정" 1주차)

## Global Constraints

- Java 21, Spring Boot 4.1.2, Gradle 9.8.2 - Groovy. 패키지는 `kr.study.ddobak`. 설정 파일은 `application.yaml`.
- 개발 OS는 Windows. 명령은 PowerShell 기준(`.\gradlew.bat`).
- 모든 API는 `/api` 아래. 오류 응답 본문은 항상 `{"message": "<한국어 설명>"}`.
- 1주차 DB는 H2 파일 모드(`./data/ddobak`). 테스트는 인메모리 H2(`src/test/resources/application.yaml`)를 써서 개발 데이터를 지우지 않는다. MySQL 전환은 이후 계획에서 한다.
- 노트 제목: 공백 불가, 최대 100자. `content`: 필수, 유효한 JSON 문자열.
- 새 노트의 기본 `content`: `{"version":1,"pages":[{"strokes":[]}]}`
- 프론트는 API 주소를 하드코딩하지 않고 항상 상대 경로 `/api/...`로 호출한다.
- Spring Boot 4는 테스트용 자동 설정 패키지 경로가 3.x와 다르다. `@AutoConfigureMockMvc`는 `org.springframework.boot.webmvc.test.autoconfigure` 패키지이고, JSON은 Jackson 3(`tools.jackson`)이다. `build.gradle`에 Initializr가 넣어준 테스트 의존성을 그대로 유지한다. AI 도구에 코드를 요청할 때 "Spring Boot 4.1, Java 21 기준"을 명시한다.

## Review Focus

1. 빈 제목, 101자 제목 → 400 + 한국어 메시지 (500이 나면 안 됨) — Task 2 테스트
2. 없는 id로 조회·수정·삭제 → 404 (500이 나면 안 됨) — Task 2, 3 테스트
3. `content`가 JSON이 아닌 문자열(`"hello"` 등) → 400 — Task 2 테스트
4. 획이 많은 큰 노트(약 1MB `content`)와 한글 제목 → 잘림·깨짐 없이 저장·조회 — Task 2 테스트
5. iPad에서 접속 시 API 호출 실패(`localhost`가 iPad 자신을 가리키는 문제, Windows 방화벽) — Task 4 수동 확인

---

### Task 1: 백엔드 DB 설정과 개발 기록 (저장소는 이미 생성·push 완료)

**Files:**
- Modify: `src/main/resources/application.yaml` (백엔드)
- Create: `src/test/resources/application.yaml` (테스트 전용 인메모리 H2)
- Modify: `.gitignore` (`data/` 추가)
- Create: `docs/dev-log/README.md`, `docs/dev-log/2026-10-31-project-setup.md`
- Create: `docs/superpowers/plans/2026-10-31-week1-note-api.md` (이 계획)

**Interfaces:**
- Produces: `http://localhost:8080`에서 실행되는 앱, 파일 DB `./data/ddobak`, H2 콘솔 `/h2-console`

- [ ] **Step 1: 설정 파일 작성** — 메인은 `jdbc:h2:file:./data/ddobak`, `ddl-auto: update`, H2 콘솔 on. 테스트는 `jdbc:h2:mem:ddobak-test`, `ddl-auto: create-drop`.
- [ ] **Step 2: 개발 기록 템플릿** — 날짜 / 만든 기능 / AI에게 준 요청 / 받은 결과 / 오류와 해결 / 내가 이해한 핵심 3줄.
- [ ] **Step 3: 확인**
  Run: `.\gradlew.bat test` → Expected: `BUILD SUCCESSFUL`
  Run: `.\gradlew.bat bootRun` → Expected: `http://localhost:8080/h2-console` 접속, JDBC URL `jdbc:h2:file:./data/ddobak`로 로그인 가능
- [ ] **Step 4: Commit** `chore: configure H2 and add dev log`

---

### Task 2: 노트 생성·단건 조회 API (검증, 오류 처리 포함)

**Files:**
- Create: `src/main/java/kr/study/ddobak/note/Note.java`
- Create: `src/main/java/kr/study/ddobak/note/NoteRepository.java`
- Create: `src/main/java/kr/study/ddobak/note/NoteRequest.java`
- Create: `src/main/java/kr/study/ddobak/note/NoteResponse.java`
- Create: `src/main/java/kr/study/ddobak/note/NoteService.java`
- Create: `src/main/java/kr/study/ddobak/note/NoteController.java`
- Create: `src/main/java/kr/study/ddobak/note/NoteNotFoundException.java`
- Create: `src/main/java/kr/study/ddobak/note/InvalidContentException.java`
- Create: `src/main/java/kr/study/ddobak/common/ErrorResponse.java`
- Create: `src/main/java/kr/study/ddobak/common/GlobalExceptionHandler.java`
- Test: `src/test/java/kr/study/ddobak/note/NoteApiTest.java`

**Interfaces:**
- Consumes: Task 1의 앱과 H2
- Produces:
  - Entity `Note`: `Long id`, `String title`(최대 100), `String content`(`@Lob`), `Instant createdAt`, `Instant updatedAt`
  - `NoteRepository extends JpaRepository<Note, Long>`
  - `record NoteRequest(@NotBlank @Size(max = 100) String title, @NotBlank String content)`
  - `record NoteResponse(Long id, String title, String content, Instant createdAt, Instant updatedAt)`
  - `record ErrorResponse(String message)`
  - `NoteService`: `NoteResponse create(NoteRequest req)`, `NoteResponse get(Long id)`
  - `NoteNotFoundException(Long id)` → 404, `InvalidContentException()` → 400
  - `POST /api/notes` → 201 + `NoteResponse`, `GET /api/notes/{id}` → 200 + `NoteResponse`

- [ ] **Step 1: 실패하는 테스트 작성**

```java
@SpringBootTest
@AutoConfigureMockMvc
class NoteApiTest {
    static final String EMPTY_CONTENT = "{\"version\":1,\"pages\":[{\"strokes\":[]}]}";

    @Autowired MockMvc mvc;
    @Autowired NoteRepository repository;

    @BeforeEach
    void clean() { repository.deleteAll(); }

    static String body(String title, String content) {
        return "{\"title\":\"" + title + "\",\"content\":\"" + content.replace("\"", "\\\"") + "\"}";
    }

    @Test
    void create_returns201WithSavedNote() throws Exception {
        mvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON)
                .content(body("전공 강의 1주차", EMPTY_CONTENT)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.title").value("전공 강의 1주차"))
            .andExpect(jsonPath("$.content").value(EMPTY_CONTENT))
            .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void get_returnsSavedNote() throws Exception { /* 생성 후 GET /api/notes/{id} → 200, title·content 일치 */ }

    @Test
    void get_unknownId_returns404() throws Exception {
        mvc.perform(get("/api/notes/999999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("노트를 찾을 수 없습니다. id=999999"));
    }

    @Test
    void create_blankTitle_returns400() throws Exception {
        mvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON)
                .content(body("  ", EMPTY_CONTENT)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void create_title101Chars_returns400() throws Exception { /* "가".repeat(101) → 400 */ }

    @Test
    void create_nonJsonContent_returns400() throws Exception {
        mvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON)
                .content(body("메모", "hello")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("필기 데이터 형식이 올바르지 않습니다."));
    }

    @Test
    void create_largeContent_savedWithoutTruncation() throws Exception {
        // strokes 배열에 점을 많이 넣어 약 1MB 크기의 content를 만든 뒤
        // 생성 → 단건 조회 → content 길이가 보낸 것과 같은지 확인
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `.\gradlew.bat test --tests "*NoteApiTest"`
Expected: FAIL (컴파일 에러: `NoteRepository`를 찾을 수 없음)

- [ ] **Step 3: `Note` 엔티티와 `NoteRepository` 구현**

`createdAt`/`updatedAt`은 `@PrePersist`/`@PreUpdate`로 채운다. `content`는 `@Lob`으로 길이 제한 없이 저장한다.

- [ ] **Step 4: DTO, 예외, `NoteService.create/get`, `NoteController` 구현**

`create`는 저장 전에 `content`를 JSON으로 파싱해 보고, 실패하면 `InvalidContentException`을 던진다. 파싱에는 Spring Boot가 등록한 JSON 매퍼 빈을 주입해 쓴다(Boot 4 기본은 Jackson 3). 컨트롤러는 `@Valid @RequestBody NoteRequest`를 받는다.

- [ ] **Step 5: `GlobalExceptionHandler` 구현**

`@RestControllerAdvice`. 매핑: `NoteNotFoundException` → 404 `"노트를 찾을 수 없습니다. id={id}"`, `InvalidContentException` → 400 `"필기 데이터 형식이 올바르지 않습니다."`, `MethodArgumentNotValidException` → 400 + 첫 번째 필드 오류 메시지.

- [ ] **Step 6: 통과 확인**

Run: `.\gradlew.bat test --tests "*NoteApiTest"`
Expected: PASS (7 tests)

- [ ] **Step 7: 개발 기록 작성 후 Commit**

```bash
git add .
git commit -m "feat: add create and get note API with validation"
```

---

### Task 3: 노트 목록·수정·삭제 API

**Files:**
- Create: `src/main/java/kr/study/ddobak/note/NoteSummary.java`
- Modify: `NoteService.java`, `NoteController.java`
- Test: `NoteApiTest.java` (테스트 추가)

**Interfaces:**
- Consumes: Task 2의 `Note`, `NoteRepository`, `NoteRequest`, `NoteResponse`, `NoteNotFoundException`
- Produces:
  - `record NoteSummary(Long id, String title, Instant updatedAt)`
  - `NoteService`: `List<NoteSummary> list()`, `NoteResponse update(Long id, NoteRequest req)`, `void delete(Long id)`
  - `GET /api/notes` → 200 + `NoteSummary[]` (`updatedAt` 최신순), `PUT /api/notes/{id}` → 200 + `NoteResponse`, `DELETE /api/notes/{id}` → 204

- [ ] **Step 1: 실패하는 테스트 추가**

```java
@Test
void list_returnsSummariesNewestFirst() throws Exception {
    // "첫 노트" 생성 → "둘째 노트" 생성 → "첫 노트" 수정
    mvc.perform(get("/api/notes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].title").value("첫 노트(수정)"))
        .andExpect(jsonPath("$[1].title").value("둘째 노트"))
        .andExpect(jsonPath("$[0].content").doesNotExist());
}

@Test
void update_changesTitleAndContent() throws Exception { /* PUT → 200, 이후 GET에서 바뀐 값 */ }

@Test
void update_unknownId_returns404() throws Exception { /* PUT /api/notes/999999 → 404 */ }

@Test
void update_nonJsonContent_returns400() throws Exception { /* PUT body("메모", "hello") → 400 */ }

@Test
void delete_returns204ThenGetReturns404() throws Exception { /* DELETE → 204, GET → 404 */ }

@Test
void delete_unknownId_returns404() throws Exception { /* DELETE /api/notes/999999 → 404 */ }
```

- [ ] **Step 2: 실패 확인**

Run: `.\gradlew.bat test --tests "*NoteApiTest"`
Expected: FAIL (새 테스트 6개가 404 또는 405로 실패)

- [ ] **Step 3: `list`, `update`, `delete`와 컨트롤러 엔드포인트 구현**

`list`는 `findAll(Sort.by(DESC, "updatedAt"))`로 정렬한다. `update`도 `create`와 같은 JSON 검사를 거친다.

- [ ] **Step 4: 통과 확인**

Run: `.\gradlew.bat test`
Expected: PASS (전체 테스트)

- [ ] **Step 5: 개발 기록 작성 후 Commit**

```bash
git add .
git commit -m "feat: add list, update, delete note API"
```

---

### Task 4: React 앱 생성, 프록시 연결, PC·iPad 확인

**Files:**
- Modify: `vite.config.js`
- Create: `src/api/notes.js`
- Create: `src/api/notes.test.js`
- Modify: `src/App.jsx`

**Interfaces:**
- Consumes: Task 2·3의 API (`/api/notes`, 오류 본문 `{message}`)
- Produces (`src/api/notes.js`, 이후 캔버스 작업이 사용):
  - `export const EMPTY_CONTENT = '{"version":1,"pages":[{"strokes":[]}]}'`
  - `listNotes(): Promise<NoteSummary[]>`
  - `getNote(id): Promise<Note>`
  - `createNote({ title, content }): Promise<Note>`
  - `updateNote(id, { title, content }): Promise<Note>`
  - `deleteNote(id): Promise<void>`
  - 응답이 2xx가 아니면 서버의 `message`를 담은 `Error`를 던진다

- [ ] **Step 1: Vitest 추가** (React 앱은 이미 생성됨)

`npm install -D vitest`, `package.json`의 `scripts`에 `"test": "vitest run"` 추가.

- [ ] **Step 2: 프록시와 외부 접속 설정 (`vite.config.js`)**

`server.host: true`(같은 와이파이의 iPad 접속 허용), `server.proxy: { '/api': 'http://localhost:8080' }`.

- [ ] **Step 3: 실패하는 테스트 작성 (`notes.test.js`)**

```js
import { describe, it, expect, vi, afterEach } from 'vitest'
import { createNote, getNote, EMPTY_CONTENT } from './notes'

afterEach(() => vi.restoreAllMocks())

describe('notes api', () => {
  it('createNote posts JSON to /api/notes and returns the note', async () => {
    const saved = { id: 1, title: '테스트', content: EMPTY_CONTENT }
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify(saved), { status: 201, headers: { 'Content-Type': 'application/json' } }))
    const result = await createNote({ title: '테스트', content: EMPTY_CONTENT })
    expect(fetchMock).toHaveBeenCalledWith('/api/notes', expect.objectContaining({ method: 'POST' }))
    expect(result).toEqual(saved)
  })

  it('throws server message on error', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ message: '노트를 찾을 수 없습니다. id=9' }), { status: 404 }))
    await expect(getNote(9)).rejects.toThrow('노트를 찾을 수 없습니다. id=9')
  })
})
```

- [ ] **Step 4: 실패 확인**

Run: `npm test`
Expected: FAIL (`./notes` 모듈 없음)

- [ ] **Step 5: `notes.js` 구현**

모든 요청은 `fetch('/api/notes...')`. `deleteNote`는 204라 본문을 읽지 않는다.

- [ ] **Step 6: 통과 확인**

Run: `npm test`
Expected: PASS (2 tests)

- [ ] **Step 7: 확인용 화면 (`App.jsx`)**

노트 목록 표시 + "새 노트" 버튼(`createNote({ title: '새 노트', content: EMPTY_CONTENT })` 후 목록 새로고침) + 각 노트 옆 "삭제" 버튼. 오류는 화면 상단에 `message`로 표시. 디자인은 신경 쓰지 않는다(필기 캔버스 작업에서 교체).

- [ ] **Step 8: PC에서 확인**

백엔드 `.\gradlew.bat bootRun`, 프론트 `npm run dev`.
Expected: `http://localhost:5173`에서 새 노트 생성·삭제가 되고, 백엔드를 껐다 켜도 목록이 남아 있음.

- [ ] **Step 9: iPad에서 확인**

PowerShell에서 `ipconfig`로 PC의 IPv4 주소(예: `192.168.0.12`)를 확인한다. iPad를 같은 와이파이에 연결하고 Safari에서 `http://<PC주소>:5173` 접속.
Expected: PC와 같은 목록이 보이고, 새 노트 생성이 된다.
접속이 안 되면: Windows 방화벽이 Node.js의 "개인 네트워크" 접근을 허용하는지 확인한다.

- [ ] **Step 10: 개발 기록 작성 후 Commit**

```bash
git add .
git commit -m "feat: add react app with notes api client and proxy"
```
