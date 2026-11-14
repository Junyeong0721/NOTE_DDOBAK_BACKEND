# 글씨 연습 AI 조언: Gemini 키 만들기

글씨 연습에서 점수는 앱이 직접 계산하고, **조언 한두 문장만** Google Gemini가 써 줍니다.
키가 없어도 연습·점수·기록은 다 되고, 조언 자리에 "가짜 조언: …"이 나옵니다.

## 돈이 나가지 않게 하는 원칙

- 키는 **결제 계정을 연결하지 않은 새 프로젝트**에 만듭니다. 결제 수단이 없으면 한도를 넘어도 거절만 되고 요금은 생길 수 없습니다.
- 글자 인식(Vision)에 쓰는 프로젝트는 무료 체험 크레딧이 연결돼 있으니 **그 프로젝트를 고르지 마세요.**
- 무료 등급에 보낸 내용은 Google이 서비스 개선에 쓸 수 있습니다. 또박은 연습 글씨 그림과 점수만 보내고 이메일·닉네임은 보내지 않습니다.

## 만드는 순서

1. <https://aistudio.google.com/app/apikey> 에 Google 계정으로 들어갑니다. (왼쪽 메뉴의 **Get API key**)
2. **Create API key**를 누릅니다.
3. 프로젝트를 고르라고 하면 **새 프로젝트 만들기(Create key in new project / Create project)**를 고릅니다. 이름은 `ddobak-advice`처럼 아무거나.
4. 만들어진 키(`AIza…`로 시작)를 복사합니다. **채팅이나 GitHub에 붙여 넣지 마세요.**
5. 백엔드 폴더의 `secret.yaml`을 열어 `ddobak:` 아래에 추가합니다. (`ocr:`와 같은 깊이)

   ```yaml
   ddobak:
     ocr:
       google:
         api-key: "…(원래 있던 Vision 키)…"
     advice:
       gemini:
         api-key: "여기에_Gemini_키"
   ```
6. 서버 창을 닫고 `start-dev.bat`으로 다시 켭니다. 서버 로그에 `연습 조언: Google Gemini (모델 이름)`이 보이면 연결된 것입니다. (`연습 조언: 가짜 조언`이면 키를 못 읽은 것)

화면 이름은 Google이 가끔 바꿉니다. 다르게 보이면 화면을 찍어 물어보세요.

## 조언이 "지금은 조언을 할 수 없어요"로만 나올 때

서버 창에서 `Gemini 호출 실패: status=…` 줄을 찾습니다.

| status | 뜻 | 할 일 |
|---|---|---|
| 404 | 모델 이름이 지금 없는 이름 | AI Studio에서 무료로 쓸 수 있는 Flash-Lite 모델 이름을 확인해 `secret.yaml`의 `ddobak.advice.gemini.model`에 적기 |
| 400 / 403 | 키가 틀렸거나 막힘 | 키를 다시 복사, 따옴표 확인 |
| 429 | Google 무료 한도(분당·하루)를 넘음 | 잠시 뒤 다시. 한도는 AI Studio의 사용량(Usage / Rate limit) 화면에서 확인 |
| (줄이 없음) | 서버의 하루 조언 횟수(회원마다 50번)를 다 씀 | 내일 다시, 또는 `ddobak.advice.daily-limit` 조정 |

모델을 바꾸려면:

```yaml
ddobak:
  advice:
    gemini:
      api-key: "…"
      model: "gemini-2.5-flash-lite"
```
