# 3주차: 글씨 보정 구현 계획

**Goal:** 한 줄을 다 쓰면 그 줄을 목표 글씨체로 바꾼 보정안을 보여 주고, "바꾸기"로 적용한다. 마음에 안 드는 글자만 "원래대로" 돌리고, 올가미로 고른 글씨도 "보정"할 수 있다. 원래 획은 지우지 않고 숨기기만 해서 언제든 되돌린다. 목표 글씨체는 사용자가 설정에서 고른다(연습 모드와 공유).

**Architecture:** 프론트만 바뀐다(백엔드 content 검사는 `version`·`pages`만 보므로 그대로). 보정 결과는 페이지에 `corrections`로 저장하되, 글자의 **위치·크기는 저장하지 않고** 그 글자를 이루는 획의 현재 위치로 매번 계산한다 → 노트 정리·올가미 이동·실행 취소를 따로 처리하지 않아도 보정 글자가 같이 움직인다. 글자는 캔버스 `fillText` + 웹 글꼴(@font-face)로 그린다(글꼴 파싱 라이브러리 불필요).

## 결정
- 글꼴: Google Fonts(OFL) 5종 — 고운돋움(기본), 나눔손글씨 펜, 하이멜로디, 감자꽃, 푸어스토리. 한글 11,172자 전부 있는 것만(개구는 2,350자라 제외). 한글·영문·기호만 남기고 woff로 줄임(0.4~1.3MB), `public/fonts/`, 라이선스 `public/fonts/OFL/`
- 설정: 기기(localStorage `ddobak.settings`)에 `{ fontId, autoSuggest }`. 라이브러리 위쪽 "설정" 버튼 → 글씨체 미리보기 카드 + "줄이 끝나면 보정안 보여 주기" 켜기/끄기
- 인식 호출(무료 한도 아끼기): 자동 보정안이 켜져 있을 때만, 다음 줄로 넘어갈 때 또는 펜을 4초 멈출 때(2.5초→4초). 이미 보정된 줄은 다시 부르지 않음. 끄면 올가미 "보정"으로만 호출

## 데이터
```js
page = { strokes: [...], corrections: [
  { id, fontId, chars: [{ ch: '가', strokeIds: ['s_..'], off?: true }] }
]}
```
- `off: true` = 이 글자만 원래대로. 숨기는 획 = `off`가 아닌 글자의 `strokeIds`
- 글자를 이루는 획이 하나라도 지워지면 그 글자는 보정에서 빠지고 남은 획이 다시 보임(`pruneCorrections`)
- 지우개가 보정 글자를 건드리면 그 글자의 획 전체를 지움

## 순수 함수 (`src/note/correction.js`)
- `buildCorrection({ ocr, origin, strokes, fontId })`: OCR 글자 상자(줄 이미지 픽셀)를 `origin {minX, minY, fit}`로 페이지 좌표로 바꾸고, 각 획을 가로 중심이 들어가는(없으면 가장 가까운) 글자에 배정. 획이 없는 글자는 버림. 형광펜 제외
- `correctedStrokeIds(corrections)`, `charGroupOf(corrections, strokeId)`
- `correctionGlyphs(strokes, corrections)`: 글자마다 `{ ch, x, y, size, color, fontId }`. x = 글자 획 상자 가운데, y = 글자 가운데들의 직선 맞춤(기울어진 줄 대응), size = 보정 전체 높이 × 1.05, 색 = 첫 획 색
- `toggleChar`, `removeCorrections(corrections, strokeIds)`, `pruneCorrections(corrections, strokes)`, `correctionText`

## 화면
- 하단 보정 막대(`CorrectionBar`): 보정안(목표 글씨체로 미리보기) + [바꾸기] [닫기] → 적용 후 글자 칩(누르면 원래대로/보정 전환) + [보정 취소] [완료]. 대상 줄은 캔버스에 노란 점선 상자
- 올가미 막대: 보정 안 된 글씨가 있으면 [보정], 보정된 글자가 있으면 [글자 고르기] [원래 글씨로]
- 줄 보기 라벨(인식 글자)은 그대로

## 검증
- 순수 함수 테스트(미니 Vitest), 빌드, Playwright로 가짜 인식 응답(글자 상자 포함)을 넣어 줄 끝 → 보정안 → 바꾸기 → 글자 원래대로 → 올가미 원래 글씨로 → 저장 데이터 확인
- PC: 머지 전 브랜치를 받아 iPad에서 확인
