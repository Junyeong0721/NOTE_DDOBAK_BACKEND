package kr.study.ddobak.note.summary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class GeminiSummaryClientTest {

    static final String URL = "https://generativelanguage.googleapis.com/v1beta/models/test-model:generateContent";
    static final JsonMapper JSON = JsonMapper.builder().build();
    static final List<byte[]> PAGES = List.of(new byte[] {1, 2, 3}, new byte[] {4, 5});

    MockRestServiceServer server;
    GeminiSummaryClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GeminiSummaryClient(builder, JSON, "test-key", "test-model");
    }

    /** Gemini 응답: 모델이 낸 글(text)이 JSON 문자열 */
    static String reply(Object result) {
        String text = result instanceof String s ? s : JSON.writeValueAsString(result);
        return JSON.writeValueAsString(Map.of("candidates", List.of(Map.of("content", Map.of("parts", List.of(Map.of("text", text)))))));
    }

    @Test
    void summarize_sendsEveryPageAndAsksForJson() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andExpect(jsonPath("$.contents[0].parts.length()").value(3))
                .andExpect(jsonPath("$.contents[0].parts[0].inline_data.mime_type").value("image/png"))
                .andExpect(jsonPath("$.contents[0].parts[0].inline_data.data").value("AQID"))
                .andExpect(jsonPath("$.contents[0].parts[1].inline_data.data").value("BAU="))
                .andExpect(jsonPath("$.contents[0].parts[2].text").value(allOf(
                        containsString("노트 2쪽"), containsString("현재 노트 제목: \"제목 없음\""),
                        containsString("3~5줄"), containsString("지어내지 말고"))))
                .andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
                .andExpect(jsonPath("$.generationConfig.responseSchema.required[0]").value("summary"))
                .andRespond(withSuccess(reply(Map.of(
                        "summary", List.of("1. 극한의 정의를 배웠다.", "- 좌극한과 우극한", "  ", "연속의 조건"),
                        "title", "「미적분 2강 극한」")), MediaType.APPLICATION_JSON));

        NoteSummaryResult r = client.summarize("제목 없음", PAGES);

        server.verify();
        assertThat(r.lines()).containsExactly("극한의 정의를 배웠다.", "좌극한과 우극한", "연속의 조건");
        assertThat(r.title()).isEqualTo("미적분 2강 극한");
    }

    @Test
    void keepsAtMostFiveLines_andDropsABadTitle() {
        server.expect(requestTo(URL)).andRespond(withSuccess(reply(Map.of(
                "summary", List.of("가", "나", "다", "라", "마", "바", "사"), "title", "제".repeat(41))), MediaType.APPLICATION_JSON));
        NoteSummaryResult r = client.summarize("t", PAGES);
        assertThat(r.lines()).containsExactly("가", "나", "다", "라", "마");
        assertThat(r.title()).isNull();
    }

    @Test
    void emptyBrokenOrTooLongReplies_fail() {
        for (String body : new String[] {"{}", reply("not json"), reply(Map.of("summary", List.of(), "title", "x")),
                reply(Map.of("summary", List.of("가".repeat(151)), "title", "x")), "garbage"}) {
            setUp();
            server.expect(requestTo(URL)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
            assertThatThrownBy(() -> client.summarize("t", PAGES)).as(body).isInstanceOf(SummaryFailedException.class);
        }
    }

    @Test
    void httpErrors_fail_withoutLeakingTheKey() {
        for (HttpStatus status : new HttpStatus[] {HttpStatus.TOO_MANY_REQUESTS, HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.NOT_FOUND}) {
            setUp();
            server.expect(requestTo(URL)).andRespond(withStatus(status));
            assertThatThrownBy(() -> client.summarize("t", PAGES))
                    .isInstanceOf(SummaryFailedException.class)
                    .hasMessageNotContaining("test-key");
        }
    }
}
