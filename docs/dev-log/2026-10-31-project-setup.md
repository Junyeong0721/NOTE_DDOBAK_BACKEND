# 2026-10-31 프로젝트 세팅

## 만든 기능
- Spring Boot 4.1.2 백엔드 프로젝트 (Java 21, Gradle) → GitHub `NOTE_DDOBAK_BACKEND`
- React 19 + Vite 8 프론트엔드 프로젝트 → GitHub `NOTE_DDOBAK_FRONTEND`
- 백엔드 H2 DB 설정 (개발용 파일 DB / 테스트용 인메모리 DB 분리)
- 노트 저장·조회·목록·수정·삭제 REST API와 테스트

## AI에게 준 요청
- 기획서(v0.5)를 바탕으로 1주차 구현 계획 작성
- Spring Initializr 설정과 의존성 추천
- React 프로젝트 생성 방법
- 프로젝트 폴더 정리, Git 초기화와 GitHub push

## 받은 결과 요약
- 설정: Gradle-Groovy / Java 21 / Spring Boot 4.1.2 / YAML
- 의존성: Spring Web, Data JPA, Validation, Lombok, DevTools, H2, MySQL Driver (Spring Security는 제외: 넣으면 모든 API가 기본으로 잠김)
- 저장소는 백엔드·프론트 2개로 분리
- iPad 설치 형태는 홈 화면 웹앱(PWA)으로 결정: Mac이 없고 무료 원칙이라, React Native는 연 $99 없이는 Expo Go 안에서만 실행되고 Capacitor는 Mac이 필요함

## 오류와 해결
| 오류 | 원인 | 해결 |
| --- | --- | --- |
| `npm : 이 시스템에서 스크립트를 실행할 수 없으므로…` | PowerShell 실행 정책이 npm.ps1 실행을 막음 | `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned` |
| 프론트 폴더에 React가 아닌 Spring Boot 파일이 있음 | Spring Boot 압축 파일을 프론트 폴더에도 풀고 이름만 바꿈 | 폴더를 비우고 `npm create vite@latest .`로 다시 생성 |
| `npm create vite` 중 `error code 3221226505` | 프로그램 비정상 종료. 한글·띄어쓰기 경로(`또박 프로젝트`) 영향 추정 | 파일은 생성됨 → `npm install`로 이어서 진행. 계속되면 영문 경로로 이동 |
| ESLint를 골랐는데 Oxlint로 설치됨 | 위 오류로 린터 교체 단계가 중단됨 | Oxlint 그대로 사용 (동작에는 영향 없음) |
| IntelliJ `does not contain a Gradle build` | 지운 빈 폴더(`ddobak`)를 IntelliJ가 계속 열고 있었음 | 프로젝트 닫고 `ddobak-backend-springboot`를 다시 열기 |
| AI가 GitHub에 push 실패 (403) | 커넥터 연결만으로는 부족, 저장소에 Claude GitHub 앱 설치 필요 | GitHub 앱을 두 저장소에 설치 |
| `bootRun` 시 `Port 8080 was already in use` | VMware 네트워크 서비스(`vmnat.exe`)가 8080 사용 (`netstat -ano \| findstr :8080` → `tasklist`로 확인) | 서비스는 그대로 두고 서버 포트를 8081로 변경, Vite 프록시도 8081로 |
| iPad에서 PC 개발 서버에 접속 불가 | PC는 KT 모뎀에 직접 연결(공인 IP `118.34.x.x`), iPad는 공유기 내부망(`172.30.1.x`)이라 서로 다른 네트워크 | Cloudflare 임시 터널(`cloudflared tunnel --url http://localhost:5173`)로 HTTPS 주소 발급, 서버는 공인 IP로 노출되지 않게 `127.0.0.1`에서만 열기. 실행은 `start-dev.bat` 한 번으로 |

## 내가 이해한 핵심 3줄
1. 오류 메시지의 마지막 줄부터 읽는다. `Port 8080 was already in use`는 코드 문제가 아니라 환경 문제였고, `netstat -ano | findstr :8080`으로 PID를 찾고 `tasklist`로 프로그램(VMware `vmnat.exe`)을 확인한 뒤, 그 서비스를 끄는 대신 내 서버 포트를 바꿨다.
2. 테스트용 DB와 개발용 DB는 분리한다. 테스트가 매번 `deleteAll()`로 데이터를 비우기 때문에, 같은 DB를 쓰면 개발하면서 만든 노트가 테스트를 돌릴 때마다 사라진다. `src/test/resources/application.yaml`로 테스트만 인메모리 H2를 쓰게 했다.
3. Vite 프록시를 쓰면 CORS 설정이 필요 없다. 브라우저는 항상 같은 주소(5173)로 `/api`를 요청하고, Vite가 뒤에서 백엔드(8081)로 넘겨주기 때문에 브라우저 입장에서는 다른 출처(origin)로 가는 요청이 아니다. 그래서 iPad에서도 같은 코드로 동작한다.
