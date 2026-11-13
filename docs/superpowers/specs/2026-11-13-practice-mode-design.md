# 연습 모드 설계 (2026-11-13)

또박의 두 번째 핵심 기능. 설정에서 고른 목표 글씨체를 따라 쓰며 연습하고, 앱이 계산한 점수와 AI 조언을 받는다.

## 1. 목표와 범위

**사용자와 정한 것**
- 첫 버전은 "기본만": 따라 쓰기, 점수, 겹쳐 보기 (+ AI 조언)
- 연습 내용: 미리 넣어 둔 단계별 세트 + 직접 입력 둘 다
- 결과는 서버 DB에 계정별로 저장
- 채점은 **A안**: 점수는 앱이 규칙으로 계산(모양·위치/크기·기울기), AI(Gemini 무료 등급)는 조언 1~2문장만
- AI 조언이 안 될 때는 "지금은 조언을 할 수 없어요"만 보여준다 (점수·기록은 그대로)
- **연습할 글씨체는 사용자가 연습 화면에서 고른다** (11/13). 처음엔 설정의 목표 글씨체로 시작하고, 연습에서 바꾼 글씨체는 연습용으로만 기억한다(노트 보정 글씨체는 그대로). 기획서의 "노트 보정과 같은 글씨체"는 이렇게 바뀜
- **Gemini에는 고른 글씨체로 그린 목표 글자와 내가 쓴 글씨를 한 그림에 같이 보낸다** (11/13)

**기획서(v0.6)에서 이어받은 것**
- 획순은 채점하지 않는다
- 반투명 가이드를 따라 쓰기 → 가이드 없이 쓰기
- iPad + Apple Pencil 기본, 폰에서는 손가락으로도 쓸 수 있게

**이번에 하지 않는 것**: 약점 글자 추천(노트 보정 기록 → 연습), 경험치·레벨·배지·연속 일수, 랭킹. 저장 구조는 나중에 이것들을 붙일 수 있게 둔다.

**성공 기준**
- iPad에서 세트를 고르고 → 칸에 따라 쓰고 → [채점하기]를 누르면 1초 안에 점수와 겹쳐 보기가 뜬다
- 같은 획이면 항상 같은 점수가 나온다
- 목표를 잘 따라 쓴 글자는 높게, 밀리거나 기울거나 획을 빼먹은 글자는 해당 항목이 낮게 나온다 (실제 손글씨로 납득되는지 사용자가 확인 후 기준값 조정)
- 키가 있으면 몇 초 안에 조언이 붙고, 키가 없거나 실패해도 흐름이 끊기지 않는다

## 2. 화면 흐름

홈의 "글씨 연습" 카드(지금 "곧 열려요") → `App`의 새 view `practice`.

### 2.1 고르기 화면 (`PracticeHome`)
- 위쪽: 연습할 글씨체 고르기(쓰기 화면과 같은 값)
- 단계 탭: [자음·모음] [글자] [단어] [문장]
- 세트 카드: 세트 글자 + 최고 점수(없으면 "아직 안 함") + 연습 횟수
- 직접 입력: 텍스트 칸(1~30자, 앞뒤 공백 제거, 한글·공백만 채점 대상) + [시작]
- 아래 "최근 연습" 목록 20개: 날짜·글·점수 → 누르면 결과 화면으로 다시 보기

### 2.2 쓰기 화면 (`PracticeBoard`)
- 상단: 글씨체 고르기(5종, 각 이름을 그 글씨체로 표시), 가이드 토글(기본 켜짐). 글씨체를 바꾸면 가이드·채점 기준이 바로 바뀌고, 이미 쓴 획은 그대로 둔다
- 원고지 칸: 한 글자 한 칸, 한 줄 최대 10칸, 30자면 3줄. 칸에는 열십자 보조선
- 가이드 켜짐: 칸 안에 목표 글자를 연하게(투명도 0.18) 그림
- 띄어쓰기는 빈칸(회색 처리), 채점·필기 대상 아님
- 입력: 획은 칸 경계를 넘어도 시작한 칸에 속한다. 펜을 한 번이라도 쓰면 그 화면에서는 손가락 입력을 무시(손바닥 방지), 펜을 안 쓰면 손가락으로 쓴다(폰)
- 버튼: [칸 지우기](선택한 칸) [전체 다시] [채점하기](한 칸도 안 썼으면 비활성)

### 2.3 결과 화면 (`PracticeResult`)
- 칸마다 겹쳐 보기: 목표 글자 회색 + 내 획 남색, 칸 아래 글자 점수
- 세트 점수(큰 숫자)와 모양/위치·크기/기울기 막대(각 항목 만점 대비 %)
- 조언 자리: "조언을 생각하는 중…" → 조언 문장 / "지금은 조언을 할 수 없어요"
- 버튼: [다시 하기](같은 세트 새로) [다음 세트](같은 단계 다음 세트, 직접 입력·마지막 세트면 숨김) [고르기로]
- 기록 다시 보기로 열었을 때는 저장된 점수·조언을 그대로 보여주고 [다시 하기] [고르기로]만

## 3. 미리 넣어 둔 세트 (`src/practice/sets.js`)

| 단계 | id | 글 |
|---|---|---|
| 자음·모음 | consonant-1 | ㄱㄴㄷㄹㅁ |
| | consonant-2 | ㅂㅅㅇㅈㅊ |
| | consonant-3 | ㅋㅌㅍㅎ |
| | vowel-1 | ㅏㅑㅓㅕㅗ |
| | vowel-2 | ㅛㅜㅠㅡㅣ |
| 글자 | syllable-1 | 가나다라마 |
| | syllable-2 | 바사아자차 |
| | syllable-3 | 카타파하 |
| | batchim-1 | 강물밥산집 |
| | batchim-2 | 길꽃닭값읽 |
| 단어 | word-1 | 강의 과제 시험 |
| | word-2 | 출석 발표 정리 |
| | word-3 | 중간고사 기말고사 |
| 문장 | sentence-1 | 오늘 배운 내용 정리 |
| | sentence-2 | 다음 주까지 과제 제출 |
| | sentence-3 | 시험 범위 꼭 확인하기 |

다섯 글꼴 모두 호환 자모(U+3131~3163) 51자가 들어 있음을 확인했다(2026-11-13).

## 4. 점수 계산 (A안, `src/practice/score.js`, 순수 함수)

글자(칸) 하나씩 계산한다.

### 4.1 모눈으로 바꾸기 (`src/practice/raster.js`)
- 칸 하나를 64×64 모눈으로 본다.
- 목표: 연습에서 고른 글씨체로 칸에 글자를 그린다. 크기는 노트 보정과 같은 방식(글꼴마다 '가' 높이 비율을 재서)으로 칸 높이의 80%. 알파 > 0.5인 모눈을 잉크로.
- 내 글씨: 그 칸의 획을 같은 좌표계로 옮겨 선 굵기 3모눈으로 그린 뒤 같은 기준으로 잉크화. (굵기를 고정하는 이유: 펜 굵기 설정과 무관하게 같은 점수)
- 브라우저 캔버스 의존은 `raster.js`에만 두고, `score.js`는 `{w, h, data: Uint8Array}` 모눈만 받는다 → 테스트에서 모눈을 직접 만들어 검사.

### 4.2 모양 (50점)
1. 내 잉크의 상자를 목표 잉크의 상자에 맞춘다: 가운데를 맞추고, 가로·세로 중 큰 쪽 비율로 **같은 비율** 확대/축소(ㅣ처럼 가는 글자가 찌그러지지 않게).
2. 기울기 보정: 가로 밀기 `x' = x − s·(y − cy)`의 s를 −0.5~0.5에서 0.05 간격으로 바꿔 보며 아래 F가 가장 큰 s를 고른다(이 s는 4.4에서 기울기로 씀). 기울기 때문에 모양 점수가 두 번 깎이지 않게 하려는 것.
3. 거리 지도(chamfer)로
   - 정확도 P = 내 잉크 중 목표 잉크에서 거리 3모눈 이내인 비율 (군더더기)
   - 재현도 R = 목표 잉크 중 내 잉크에서 거리 3모눈 이내인 비율 (빠뜨림)
   - F = 2PR / (P + R)
4. 모양 = 50 × clamp((F − 0.30) / (0.95 − 0.30), 0, 1)

### 4.3 위치·크기 (25점) — 맞추기 전 원래 자리로
- 중심 거리 d = 내 잉크 상자 중심과 목표 잉크 상자 중심의 거리 ÷ 칸 한 변
- 크기 비 r = 내 상자 대각선 ÷ 목표 상자 대각선
- 위치·크기 = 25 × (0.5 × clamp(1 − d / 0.25) + 0.5 × clamp(1 − |ln r| / ln 1.6))

### 4.4 기울기 (25점)
- 기울기 각 θ = |atan(s)| (4.2의 최적 s), 3° 이하는 0으로 봄
- 기울기 = 25 × clamp(1 − (θ − 3°) / 17°)  (20°면 0점)

### 4.5 합치기
- 글자 점수 = 모양 + 위치·크기 + 기울기, 반올림 정수 0~100
- 안 쓴 칸은 0점, "안 씀" 표시
- 세트 점수 = 채점 대상 칸(공백 제외) 글자 점수의 평균. 항목 점수도 같은 방식으로 평균 후 각 만점 대비 %로 표시
- 위 숫자(0.30, 0.95, 3모눈, 0.25, 1.6, 3°, 20°)는 `SCORE_TUNING` 상수 하나에 모아 두고, 실제 손글씨로 확인한 뒤 조정한다.

## 5. AI 조언 (Gemini 무료 등급)

### 5.1 흐름
1. [채점하기] → 앱이 즉시 점수를 계산하고 결과 화면 표시
2. 앱이 조언용 그림(`src/practice/adviceImage.js`)을 만든다: 고른 글씨체로 그린 목표 글자와 내가 쓴 글씨를 **한 그림에 같이** 넣는다. 위 줄 = 목표(왼쪽에 "목표" 표시), 아래 줄 = 내 글씨("내 글씨" 표시), 글자마다 칸 위치를 위아래로 맞춘 PNG(칸 96px, 흰 배경). 겹친 그림은 사람이 보기 좋지만 AI가 헷갈릴 수 있어 위아래로 나눈다. 10칸이 넘으면 목표 줄/내 글씨 줄 쌍을 줄마다 반복
3. `POST /api/practice/attempts` (점수 + 획 + 그림)
4. 서버: 검증 → **기록 먼저 저장** → 하루 한도 확인 → Gemini 호출(10초 제한) → 성공 시 조언을 기록에 저장 → 응답
5. 앱이 조언 자리를 채운다

### 5.2 Gemini 요청
- `POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`, 키는 `x-goog-api-key` 헤더 (Vision과 같은 방식)
- 모델은 `ddobak.advice.gemini.model`(기본: 무료 등급·그림 입력 가능한 Flash-Lite 계열, 구현 때 AI Studio에서 확인한 이름)
- 내용: 그림(inline PNG) + 글: 목표 글씨체 이름, 연습한 글, 가이드 여부, 글자별 점수(모양/위치·크기/기울기), 지시
  - "위 줄은 목표, 아래 줄은 학습자 손글씨"
  - "점수가 가장 낮은 글자와 항목 위주로, 고치는 방법을 한국어 1~2문장, 120자 이내로"
  - "획순은 언급하지 말 것", "점수를 다시 매기지 말 것"
- `generationConfig`: temperature 0.4, 최대 출력 짧게. 응답 글이 비었거나 300자를 넘으면 실패로 처리(300자에서 자르지 않음)

### 5.3 실패와 한도
- 실패 = 키 없음이 아닌데 호출 오류·10초 초과·빈 응답·하루 한도 초과
- 실패하면 `advice = null`, 응답 `adviceStatus: "UNAVAILABLE"` → 화면 "지금은 조언을 할 수 없어요". 기록과 점수는 그대로
- 하루 한도: **계정마다** `ddobak.advice.daily-limit`(기본 50), Asia/Seoul 날짜 기준, 서버 메모리(재시작하면 초기화 — 글자 인식 한도와 같은 한계)
- 키가 비어 있으면 `FakeAdviceClient`가 "가짜 조언: …" 문장을 돌려준다(개발·테스트용)
- 키는 `secret.yaml`의 `ddobak.advice.gemini.api-key`(환경 변수 `GEMINI_API_KEY`도 가능). 결제 계정이 **없는** 별도 프로젝트에서 AI Studio로 만든다 → 한도를 넘어도 요금이 생길 수 없음
- 무료 등급에 보낸 데이터는 Google이 서비스 개선에 쓸 수 있다(연습 글씨 그림과 점수만 보내고, 이메일·닉네임은 보내지 않음)

## 6. 서버

### 6.1 DB — `V2__create_practice_attempt.sql`
```sql
CREATE TABLE practice_attempt (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    user_id        BIGINT       NOT NULL COMMENT '주인 회원',
    set_id         VARCHAR(40)  NULL     COMMENT '미리 넣어 둔 세트 id. 직접 입력이면 NULL',
    target_text    VARCHAR(30)  NOT NULL COMMENT '연습한 글 (API에서는 text)',
    font_id        VARCHAR(40)  NOT NULL COMMENT '목표 글씨체',
    guide          BOOLEAN      NOT NULL COMMENT '가이드를 켜고 썼는지',
    strokes        LONGTEXT     NOT NULL COMMENT '칸별 획 JSON (다시 보기용)',
    char_scores    TEXT         NOT NULL COMMENT '글자별 점수 JSON',
    total_score    INT          NOT NULL COMMENT '세트 점수 0~100',
    shape_score    INT          NOT NULL COMMENT '모양 % 0~100',
    position_score INT          NOT NULL COMMENT '위치·크기 % 0~100',
    tilt_score     INT          NOT NULL COMMENT '기울기 % 0~100',
    advice         VARCHAR(300) NULL     COMMENT 'AI 조언. 실패했으면 NULL',
    created_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_practice_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
CREATE INDEX idx_practice_user_created ON practice_attempt (user_id, created_at);
CREATE INDEX idx_practice_user_text ON practice_attempt (user_id, target_text);
```
- 조언용 그림은 저장하지 않는다(획으로 다시 그릴 수 있음)
- 점수는 INT(Hibernate validate와 맞추기 쉬움), 글 칸 이름은 SQL 예약어 혼동을 피해 `target_text`. `guide` BOOLEAN이 MySQL(tinyint(1))·H2 validate를 둘 다 통과하는지 구현 때 확인
- 테스트 `application.yaml`의 `spring.sql.init.schema-locations`에 V2 추가, `NoteSchemaTest`처럼 엔티티·SQL 일치 테스트

### 6.2 API (모두 로그인 필요, 남의 기록은 404)

| 주소 | 하는 일 |
|---|---|
| `POST /api/practice/attempts` | 저장 + 조언 → 201, 기록 + `advice` + `adviceStatus`(`OK`/`UNAVAILABLE`) |
| `GET /api/practice/best` | 글(text)마다 최고 점수·횟수·마지막 날짜 |
| `GET /api/practice/attempts` | 최근 20개 요약(획 제외) |
| `GET /api/practice/attempts/{id}` | 기록 하나(획 포함) |

`POST` 요청 검증(400 + 한국어 메시지): text 1~30자, fontId 필수, cells 개수 = text 글자 수, 점수 0~100, strokes JSON 2MB 이하, image는 `data:image/png;base64,` 1MB 이하.

### 6.3 코드 구조 (`kr.study.ddobak.practice`)
- `PracticeAttempt`(엔티티), `PracticeAttemptRepository`, `PracticeService`, `PracticeController`, 요청/응답 DTO
- `advice/`: `AdviceClient`(인터페이스: 그림 + 글 → 조언 문자열 또는 실패), `GeminiAdviceClient`(RestClient), `FakeAdviceClient`, `AdviceConfig`(키 비면 Fake), `AdviceUsageLimiter`(계정·날짜별), `AdviceFailedException`
- 조언 부분을 인터페이스로 떼어 두어 나중에 다른 AI(GPT 등)로 바꿀 때 `AdviceClient` 구현만 갈아 끼운다

## 7. 프론트 구조
- `src/practice/`: `practiceFont.js`(연습용 글씨체 기억, localStorage `ddobak.practice` — 없으면 설정 글씨체), `sets.js`, `cells.js`(칸 배치·획→칸 배정), `raster.js`, `score.js`, `adviceImage.js`, `pen.js`(펜이 쓰였으면 손가락 무시)
- `src/components/practice/`: `PracticeHome.jsx`, `PracticeBoard.jsx`, `PracticeResult.jsx`, `Practice.css`
- `src/api/practice.js`: 위 4개 API (401이면 기존처럼 `ddobak:unauthorized`)
- `Home.jsx`의 "곧 열려요" 제거, 카드 → `practice` view. `Landing.jsx`의 `soon` 제거

## 8. 테스트
- **점수(가장 촘촘히)**: 모눈을 직접 만들어
  - 목표와 같은 잉크 → 95점 이상
  - 옆으로 밀림 → 위치만 깎이고 모양·기울기는 거의 그대로
  - 2배 크기 → 위치·크기만 깎임
  - 비스듬히 민 글자 → 기울기만 깎임
  - 획 하나 빠짐 → 모양 깎임
  - 낙서·빈칸 → 낮음/0점
  - 같은 입력 → 같은 점수
- **칸 배치**: 10칸 줄바꿈, 공백 칸, 획 시작점으로 칸 배정
- **서버**: 저장·조회·최고 점수, 남의 기록 404, 검증 400, 조언 실패해도 저장(advice null, UNAVAILABLE), 계정별 하루 한도, Gemini 요청 형식(MockRestServiceServer), V2 표·엔티티 일치
- **화면**: Playwright + 가짜 서버로 홈 → 고르기 → 쓰기(가상 펜 입력) → 결과 → 다시 보기
- **실제**: Gemini 키를 넣고 iPad에서 직접 써 보며 점수 체감 확인 → `SCORE_TUNING` 조정

## 9. 사용자가 할 일
1. (구현 끝난 뒤) Google AI Studio에서 결제 계정 없는 새 프로젝트로 Gemini API 키 만들기 → `secret.yaml`에 넣기 (화면별 안내 예정)
2. iPad에서 연습해 보고 점수가 납득되는지 알려주기
