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

## 내가 이해한 핵심 3줄
1. (직접 작성)
2. 
3. 
