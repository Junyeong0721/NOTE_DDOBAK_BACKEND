package kr.study.ddobak.practice.advice;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Google Gemini(무료 등급)에게 그림과 점수를 보내 조언을 받는다.
 * API 키는 URL이 아니라 헤더로 보내서 오류 로그에 키가 찍히지 않게 한다.
 */
@Slf4j
public class GeminiAdviceClient implements AdviceClient {

    static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent";
    static final int MAX_ADVICE_LENGTH = 300;

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final String apiKey;
    private final String model;

    public GeminiAdviceClient(RestClient.Builder builder, JsonMapper jsonMapper, String apiKey, String model) {
        this.restClient = builder.build();
        this.jsonMapper = jsonMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String advise(AdviceRequest request) {
        String response;
        try {
            response = restClient.post()
                    .uri(ENDPOINT, model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody(request))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            log.warn("Gemini 호출 실패: status={} body={}", e.getStatusCode().value(), shorten(e.getResponseBodyAsString()));
            throw new AdviceFailedException("조언 서버가 요청을 받지 않았습니다. (" + e.getStatusCode().value() + ")");
        } catch (RestClientException e) {
            log.warn("Gemini 연결 실패: {}", e.getMessage());
            throw new AdviceFailedException("조언 서버에 연결하지 못했습니다.");
        }
        return parse(response);
    }

    private String requestBody(AdviceRequest request) {
        Map<String, Object> image = Map.of("inline_data",
                Map.of("mime_type", "image/png", "data", Base64.getEncoder().encodeToString(request.png())));
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("role", "user", "parts", List.of(image, Map.of("text", prompt(request))))),
                "generationConfig", Map.of("temperature", 0.4, "maxOutputTokens", 1024));
        return jsonMapper.writeValueAsString(body);
    }

    static String prompt(AdviceRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("한글 손글씨 연습 결과입니다. 그림에는 문제마다 두 줄이 있습니다: '목표' 줄은 따라 쓸 글씨체(")
                .append(request.fontLabel())
                .append(")로 쓴 글자이고, 그 아래 '내 글씨' 줄은 학습자가 같은 칸에 직접 쓴 글씨입니다.\n");
        sb.append("연습 단계: ").append(request.stageLabel()).append(", 문제 ").append(request.items().size()).append("개\n");
        sb.append(request.guide() ? "연한 목표 글자를 보면서 따라 썼습니다.\n" : "목표 글자를 보지 않고 썼습니다.\n");
        sb.append("앱이 계산한 점수 (글자마다 모양 50점, 위치와 크기 25점, 기울기 25점 만점):\n");
        int n = 1;
        for (AdviceRequest.Item item : request.items()) {
            sb.append(n++).append("번 '").append(item.text()).append("' (문제 점수 ").append(item.total()).append(")\n");
            for (AdviceRequest.CharScore s : item.scores()) {
                sb.append("  - ").append(s.ch()).append(": ");
                if (s.empty()) {
                    sb.append("쓰지 않음\n");
                } else {
                    sb.append("합계 ").append(s.total())
                            .append(", 모양 ").append(s.shape())
                            .append(", 위치와 크기 ").append(s.position())
                            .append(", 기울기 ").append(s.tilt()).append('\n');
                }
            }
        }
        sb.append("""
                점수가 가장 낮은 글자와 항목을 중심으로, 그림을 보고 어디를 어떻게 고치면 목표 글씨체와 비슷해지는지 알려 주세요.
                규칙: 한국어 해요체로 1~2문장, 120자 이내. 획순은 언급하지 마세요. 점수를 다시 매기거나 숫자를 나열하지 마세요. \
                인사말이나 머리말 없이 조언만 쓰세요.""");
        return sb.toString();
    }

    String parse(String response) {
        JsonNode root;
        try {
            root = jsonMapper.readTree(response == null ? "{}" : response);
        } catch (JacksonException e) {
            log.warn("Gemini 응답을 읽지 못함: {}", shorten(response));
            throw new AdviceFailedException("조언 응답을 읽지 못했습니다.");
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode part : root.path("candidates").path(0).path("content").path("parts")) {
            text.append(part.path("text").asString(""));
        }
        String advice = text.toString().replaceAll("\\s+", " ").strip();
        if (advice.isEmpty()) {
            log.warn("Gemini가 빈 조언을 돌려줌: {}", shorten(response));
            throw new AdviceFailedException("조언이 비어 있습니다.");
        }
        if (advice.length() > MAX_ADVICE_LENGTH) {
            log.warn("Gemini 조언이 너무 김: {}자", advice.length());
            throw new AdviceFailedException("조언이 너무 깁니다.");
        }
        return advice;
    }

    private static String shorten(String s) {
        if (s == null) return "";
        return s.length() > 300 ? s.substring(0, 300) + "…" : s;
    }
}
