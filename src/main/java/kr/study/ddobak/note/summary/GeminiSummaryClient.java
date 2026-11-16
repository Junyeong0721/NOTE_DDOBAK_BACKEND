package kr.study.ddobak.note.summary;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
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
 * Google Gemini(무료 등급)에게 노트 페이지 그림을 보내 손글씨를 읽고 요약하게 한다.
 * 답은 정해진 JSON 형식({ summary: [줄...], title })으로만 받는다.
 */
@Slf4j
public class GeminiSummaryClient implements SummaryClient {

    static final String ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent";
    static final int MAX_LINES = 5;
    static final int MAX_LINE_LENGTH = 150;
    static final int MAX_TITLE_LENGTH = 40;

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final String apiKey;
    private final String model;

    public GeminiSummaryClient(RestClient.Builder builder, JsonMapper jsonMapper, String apiKey, String model) {
        this.restClient = builder.build();
        this.jsonMapper = jsonMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public NoteSummaryResult summarize(String currentTitle, List<byte[]> pages) {
        String response;
        try {
            response = restClient.post()
                    .uri(ENDPOINT, model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody(currentTitle, pages))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            log.warn("Gemini 요약 호출 실패: status={} body={}", e.getStatusCode().value(), shorten(e.getResponseBodyAsString()));
            throw new SummaryFailedException("요약 서버가 요청을 받지 않았습니다. (" + e.getStatusCode().value() + ")");
        } catch (RestClientException e) {
            log.warn("Gemini 요약 연결 실패: {}", e.getMessage());
            throw new SummaryFailedException("요약 서버에 연결하지 못했습니다.");
        }
        return parse(response);
    }

    private String requestBody(String currentTitle, List<byte[]> pages) {
        List<Object> parts = new ArrayList<>();
        for (byte[] png : pages) {
            parts.add(Map.of("inline_data", Map.of("mime_type", "image/png", "data", Base64.getEncoder().encodeToString(png))));
        }
        parts.add(Map.of("text", prompt(currentTitle, pages.size())));
        Map<String, Object> schema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "summary", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")),
                        "title", Map.of("type", "STRING")),
                "required", List.of("summary", "title"));
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("temperature", 0.3);
        config.put("maxOutputTokens", 2048);
        config.put("responseMimeType", "application/json");
        config.put("responseSchema", schema);
        return jsonMapper.writeValueAsString(Map.of(
                "contents", List.of(Map.of("role", "user", "parts", parts)),
                "generationConfig", config));
    }

    static String prompt(String currentTitle, int pageCount) {
        return """
                대학생이 아이패드로 손글씨 필기한 노트 %d쪽의 그림입니다. 현재 노트 제목: "%s"
                손글씨를 읽고 노트 내용을 정리해 주세요.
                - summary: 핵심 내용을 한국어 3~5줄로. 한 줄은 60자 이내의 짧은 문장, 줄 앞에 번호나 기호를 붙이지 마세요.
                - title: 내용에 맞는 노트 제목 하나, 20자 이내. (예: "미적분 2강 극한의 정의")
                필기에 없는 내용은 지어내지 말고, 읽기 어려운 부분은 빼세요. 글씨가 거의 없으면 summary에 읽을 수 있는 내용만 한두 줄로 쓰세요."""
                .formatted(pageCount, currentTitle == null ? "" : currentTitle.replace("\"", "'"));
    }

    NoteSummaryResult parse(String response) {
        String text;
        try {
            JsonNode root = jsonMapper.readTree(response == null ? "{}" : response);
            StringBuilder sb = new StringBuilder();
            for (JsonNode part : root.path("candidates").path(0).path("content").path("parts")) {
                sb.append(part.path("text").asString(""));
            }
            text = sb.toString().strip();
        } catch (JacksonException e) {
            log.warn("Gemini 요약 응답을 읽지 못함: {}", shorten(response));
            throw new SummaryFailedException("요약 응답을 읽지 못했습니다.");
        }
        JsonNode result;
        try {
            result = jsonMapper.readTree(text.isEmpty() ? "{}" : text);
        } catch (JacksonException e) {
            log.warn("Gemini 요약이 JSON이 아님: {}", shorten(text));
            throw new SummaryFailedException("요약 형식이 올바르지 않습니다.");
        }
        List<String> lines = new ArrayList<>();
        for (JsonNode line : result.path("summary")) {
            String l = line.asString("").replaceAll("\\s+", " ").replaceFirst("^(\\d+[.)]|[-•·*])\\s*", "").strip();
            if (l.isEmpty()) continue;
            if (l.length() > MAX_LINE_LENGTH) {
                log.warn("Gemini 요약 줄이 너무 김: {}자", l.length());
                throw new SummaryFailedException("요약이 너무 깁니다.");
            }
            if (lines.size() < MAX_LINES) lines.add(l);
        }
        if (lines.isEmpty()) {
            log.warn("Gemini가 빈 요약을 돌려줌: {}", shorten(text));
            throw new SummaryFailedException("요약이 비어 있습니다.");
        }
        String title = result.path("title").asString("").replaceAll("\\s+", " ").replaceAll("^[\"'「『]+|[\"'」』]+$", "").strip();
        return new NoteSummaryResult(lines, title.isEmpty() || title.length() > MAX_TITLE_LENGTH ? null : title);
    }

    private static String shorten(String s) {
        if (s == null) return "";
        return s.length() > 300 ? s.substring(0, 300) + "…" : s;
    }
}
