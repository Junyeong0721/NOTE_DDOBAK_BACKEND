package kr.study.ddobak.ocr;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Google Cloud Vision의 DOCUMENT_TEXT_DETECTION(손글씨 포함 문서 인식)으로 한 줄 이미지를 읽는다.
 * 글자(symbol)마다 위치와 신뢰도를 돌려받아 OcrChar로 바꾼다.
 * API 키는 URL이 아니라 헤더로 보내서 오류 로그에 키가 찍히지 않게 한다.
 */
@Slf4j
public class GoogleVisionOcrClient implements OcrClient {

    static final String ENGINE = "google-vision";
    static final String ENDPOINT = "https://vision.googleapis.com/v1/images:annotate";

    static final String SETUP_MESSAGE = "글자 인식 설정을 확인해 주세요. (API 키, 결제, Cloud Vision API 사용 설정)";
    static final String CONNECT_MESSAGE = "글자 인식 서버에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.";
    static final String FAILED_MESSAGE = "글자를 인식하지 못했습니다.";
    static final String QUOTA_MESSAGE = "글자 인식 서버의 사용 한도를 넘었습니다. 잠시 후 다시 시도해 주세요.";

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final String apiKey;

    public GoogleVisionOcrClient(RestClient.Builder builder, JsonMapper jsonMapper, String apiKey) {
        this.restClient = builder.build();
        this.jsonMapper = jsonMapper;
        this.apiKey = apiKey;
    }

    @Override
    public OcrResult recognize(byte[] png, int width, int height) {
        String response;
        try {
            response = restClient.post()
                    .uri(ENDPOINT)
                    .header("X-Goog-Api-Key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody(png))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            log.warn("Google Vision 호출 실패: status={} body={}", e.getStatusCode().value(), shorten(e.getResponseBodyAsString()));
            throw toException(e.getStatusCode().value());
        } catch (RestClientException e) {
            log.warn("Google Vision 연결 실패: {}", e.getMessage());
            throw new OcrFailedException(CONNECT_MESSAGE);
        }
        return parse(response);
    }

    private String requestBody(byte[] png) {
        Map<String, Object> request = Map.of(
                "image", Map.of("content", Base64.getEncoder().encodeToString(png)),
                "features", List.of(Map.of("type", "DOCUMENT_TEXT_DETECTION")),
                "imageContext", Map.of("languageHints", List.of("ko")));
        return jsonMapper.writeValueAsString(Map.of("requests", List.of(request)));
    }

    private RuntimeException toException(int status) {
        if (status == HttpStatus.TOO_MANY_REQUESTS.value()) {
            return new OcrLimitExceededException(QUOTA_MESSAGE);
        }
        if (status == 400 || status == 401 || status == 403) {
            return new OcrFailedException(SETUP_MESSAGE); // 잘못된 키, 결제 미연결, API 미사용 등
        }
        return new OcrFailedException(CONNECT_MESSAGE);
    }

    OcrResult parse(String response) {
        JsonNode result;
        try {
            result = jsonMapper.readTree(response == null ? "{}" : response).path("responses").path(0);
        } catch (JacksonException e) {
            log.warn("Google Vision 응답을 읽지 못함: {}", shorten(response));
            throw new OcrFailedException(FAILED_MESSAGE);
        }
        if (result.has("error")) {
            log.warn("Google Vision 인식 오류: {}", result.path("error").path("message").asString(""));
            throw new OcrFailedException(FAILED_MESSAGE);
        }

        JsonNode annotation = result.path("fullTextAnnotation");
        String text = annotation.path("text").asString("").replaceAll("\\s+", " ").trim();
        List<OcrChar> chars = new ArrayList<>();
        for (JsonNode page : annotation.path("pages")) {
            for (JsonNode block : page.path("blocks")) {
                for (JsonNode paragraph : block.path("paragraphs")) {
                    for (JsonNode word : paragraph.path("words")) {
                        for (JsonNode symbol : word.path("symbols")) {
                            OcrChar ch = toChar(symbol);
                            if (ch != null) chars.add(ch);
                        }
                    }
                }
            }
        }
        return new OcrResult(ENGINE, text, chars);
    }

    /** symbol의 boundingBox 꼭짓점 4개로 상자를 만든다. 값이 0인 좌표는 응답에서 빠지므로 0으로 본다. */
    private static OcrChar toChar(JsonNode symbol) {
        String ch = symbol.path("text").asString("");
        if (ch.isBlank()) return null;
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (JsonNode v : symbol.path("boundingBox").path("vertices")) {
            double x = v.path("x").asDouble(0);
            double y = v.path("y").asDouble(0);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
        }
        if (minX == Double.MAX_VALUE) {
            minX = minY = maxX = maxY = 0;
        }
        return new OcrChar(ch, minX, minY, maxX - minX, maxY - minY, symbol.path("confidence").asDouble(0));
    }

    private static String shorten(String s) {
        if (s == null) return "";
        return s.length() > 300 ? s.substring(0, 300) + "…" : s;
    }
}
