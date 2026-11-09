package kr.study.ddobak.ocr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class GoogleVisionOcrClientTest {

    static final byte[] PNG = Base64.getDecoder().decode(OcrApiTest.ONE_PIXEL_PNG);

    MockRestServiceServer server;
    GoogleVisionOcrClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GoogleVisionOcrClient(builder, JsonMapper.builder().build(), "test-key");
    }

    @Test
    void recognize_sendsDocumentTextDetectionWithKeyHeader_andParsesSymbols() {
        // "가나 다": 첫 글자는 x=0 꼭짓점이 응답에서 빠진 경우
        String response = """
                {"responses":[{"fullTextAnnotation":{"text":"가나 다\\n","pages":[{"blocks":[{"paragraphs":[{"words":[
                  {"symbols":[
                    {"text":"가","confidence":0.9,"boundingBox":{"vertices":[{"y":2},{"x":20,"y":2},{"x":20,"y":30},{"y":30}]}},
                    {"text":"나","confidence":0.8,"boundingBox":{"vertices":[{"x":22,"y":3},{"x":40,"y":3},{"x":40,"y":31},{"x":22,"y":31}]}}]},
                  {"symbols":[
                    {"text":"다","confidence":0.5,"boundingBox":{"vertices":[{"x":50,"y":1},{"x":70,"y":1},{"x":70,"y":29},{"x":50,"y":29}]}}]}
                ]}]}]}]}}]}
                """;
        server.expect(requestTo(GoogleVisionOcrClient.ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Goog-Api-Key", "test-key"))
                .andExpect(jsonPath("$.requests[0].image.content").value(OcrApiTest.ONE_PIXEL_PNG))
                .andExpect(jsonPath("$.requests[0].features[0].type").value("DOCUMENT_TEXT_DETECTION"))
                .andExpect(jsonPath("$.requests[0].imageContext.languageHints[0]").value("ko"))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        OcrResult result = client.recognize(PNG, 80, 40);

        server.verify();
        assertThat(result.engine()).isEqualTo("google-vision");
        assertThat(result.text()).isEqualTo("가나 다");
        assertThat(result.chars()).extracting(OcrChar::ch).containsExactly("가", "나", "다");
        assertThat(result.chars().get(0)).isEqualTo(new OcrChar("가", 0, 2, 20, 28, 0.9));
        assertThat(result.chars().get(2).confidence()).isEqualTo(0.5);
    }

    @Test
    void recognize_noTextFound_returnsEmpty() {
        server.expect(requestTo(GoogleVisionOcrClient.ENDPOINT))
                .andRespond(withSuccess("{\"responses\":[{}]}", MediaType.APPLICATION_JSON));

        OcrResult result = client.recognize(PNG, 1, 1);

        assertThat(result.text()).isEmpty();
        assertThat(result.chars()).isEmpty();
    }

    @Test
    void recognize_errorInsideResponse_throwsFailed() {
        server.expect(requestTo(GoogleVisionOcrClient.ENDPOINT))
                .andRespond(withSuccess("{\"responses\":[{\"error\":{\"code\":3,\"message\":\"Bad image data.\"}}]}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.recognize(PNG, 1, 1))
                .isInstanceOf(OcrFailedException.class)
                .hasMessage(GoogleVisionOcrClient.FAILED_MESSAGE);
    }

    @Test
    void recognize_429_throwsLimitExceeded() {
        server.expect(requestTo(GoogleVisionOcrClient.ENDPOINT))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.recognize(PNG, 1, 1))
                .isInstanceOf(OcrLimitExceededException.class)
                .hasMessage(GoogleVisionOcrClient.QUOTA_MESSAGE);
    }

    @Test
    void recognize_403_throwsSetupMessage() {
        // 결제 미연결·API 미사용·키 제한 등
        server.expect(requestTo(GoogleVisionOcrClient.ENDPOINT))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .body("{\"error\":{\"message\":\"This API method requires billing to be enabled.\"}}"));

        assertThatThrownBy(() -> client.recognize(PNG, 1, 1))
                .isInstanceOf(OcrFailedException.class)
                .hasMessage(GoogleVisionOcrClient.SETUP_MESSAGE);
    }

    @Test
    void recognize_500_throwsConnectMessage() {
        server.expect(requestTo(GoogleVisionOcrClient.ENDPOINT))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.recognize(PNG, 1, 1))
                .isInstanceOf(OcrFailedException.class)
                .hasMessage(GoogleVisionOcrClient.CONNECT_MESSAGE);
    }
}
