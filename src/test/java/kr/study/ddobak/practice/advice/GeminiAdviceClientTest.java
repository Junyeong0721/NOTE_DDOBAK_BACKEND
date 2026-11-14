package kr.study.ddobak.practice.advice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

class GeminiAdviceClientTest {

    static final String PNG_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==";
    static final String URL = "https://generativelanguage.googleapis.com/v1beta/models/test-model:generateContent";

    static final AdviceRequest REQUEST = new AdviceRequest("고운돋움", "가나", true,
            List.of(new AdviceRequest.CharScore("가", 62, 30, 20, 12, false),
                    new AdviceRequest.CharScore("나", 0, 0, 0, 0, true)),
            Base64.getDecoder().decode(PNG_BASE64));

    MockRestServiceServer server;
    GeminiAdviceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GeminiAdviceClient(builder, JsonMapper.builder().build(), "test-key", "test-model");
    }

    static String reply(String text) {
        return JsonMapper.builder().build().writeValueAsString(java.util.Map.of("candidates",
                List.of(java.util.Map.of("content", java.util.Map.of("parts", List.of(java.util.Map.of("text", text)))))));
    }

    @Test
    void advise_sendsImageScoresAndRules_withKeyInHeader() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andExpect(jsonPath("$.contents[0].parts[0].inline_data.mime_type").value("image/png"))
                .andExpect(jsonPath("$.contents[0].parts[0].inline_data.data").value(PNG_BASE64))
                .andExpect(jsonPath("$.contents[0].parts[1].text").value(allOf(
                        containsString("고운돋움"),
                        containsString("연습한 글: 가나"),
                        containsString("연한 목표 글자를 보면서"),
                        containsString("- 가: 합계 62, 모양 30, 위치와 크기 20, 기울기 12"),
                        containsString("- 나: 쓰지 않음"),
                        containsString("획순은 언급하지 마세요"),
                        containsString("120자 이내"),
                        not(containsString("test-key")))))
                .andExpect(jsonPath("$.generationConfig.temperature").value(0.4))
                .andRespond(withSuccess(reply("  '가'의 세로 획이 오른쪽으로 기울었어요.\n곧게 내려 보세요. "), MediaType.APPLICATION_JSON));

        String advice = client.advise(REQUEST);

        server.verify();
        assertThat(advice).isEqualTo("'가'의 세로 획이 오른쪽으로 기울었어요. 곧게 내려 보세요.");
    }

    @Test
    void advise_joinsSeveralParts() {
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"받침을 \"},{\"text\":\"조금 크게 써 보세요.\"}]}}]}",
                MediaType.APPLICATION_JSON));

        assertThat(client.advise(REQUEST)).isEqualTo("받침을 조금 크게 써 보세요.");
    }

    @Test
    void advise_withoutGuide_saysSo() {
        assertThat(GeminiAdviceClient.prompt(new AdviceRequest("감자꽃", "가", false, List.of(), new byte[0])))
                .contains("목표 글자를 보지 않고 썼습니다.");
    }

    @Test
    void emptyOrBrokenReplies_fail() {
        for (String body : new String[] {"{}", "{\"candidates\":[]}", reply("   "), "not json",
                "{\"candidates\":[{\"finishReason\":\"SAFETY\"}]}"}) {
            setUp();
            server.expect(requestTo(URL)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
            assertThatThrownBy(() -> client.advise(REQUEST)).as(body).isInstanceOf(AdviceFailedException.class);
        }
    }

    @Test
    void tooLongReply_fails_ratherThanBeingCut() {
        server.expect(requestTo(URL)).andRespond(withSuccess(reply("가".repeat(301)), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.advise(REQUEST)).isInstanceOf(AdviceFailedException.class);

        setUp();
        server.expect(requestTo(URL)).andRespond(withSuccess(reply("가".repeat(300)), MediaType.APPLICATION_JSON));
        assertThat(client.advise(REQUEST)).hasSize(300);
    }

    @Test
    void httpErrors_fail_withoutLeakingTheKey() {
        for (HttpStatus status : new HttpStatus[] {HttpStatus.TOO_MANY_REQUESTS, HttpStatus.INTERNAL_SERVER_ERROR,
                HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND}) {
            setUp();
            server.expect(requestTo(URL)).andRespond(withStatus(status).body("{\"error\":{\"message\":\"nope\"}}"));
            assertThatThrownBy(() -> client.advise(REQUEST))
                    .isInstanceOf(AdviceFailedException.class)
                    .hasMessageNotContaining("test-key");
        }
    }
}
