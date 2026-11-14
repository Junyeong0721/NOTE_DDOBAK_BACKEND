package kr.study.ddobak.practice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import kr.study.ddobak.practice.advice.AdviceClient;
import kr.study.ddobak.practice.advice.AdviceFailedException;
import kr.study.ddobak.practice.advice.AdviceRequest;
import kr.study.ddobak.support.TestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

/** 조언 AI를 흉내 내는 클라이언트로 바꿔서, 조언 성공·실패·하루 한도(2번)일 때 기록이 어떻게 되는지 본다. */
@SpringBootTest(properties = "ddobak.advice.daily-limit=2")
@AutoConfigureMockMvc
class PracticeAdviceApiTest {

    static final String IMAGE = "data:image/png;base64,"
            + "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==";

    /** 받은 요청을 기억하고, fail이 켜져 있으면 실패한다 */
    static class StubAdviceClient implements AdviceClient {
        final List<AdviceRequest> requests = new ArrayList<>();
        RuntimeException failure;

        @Override
        public String advise(AdviceRequest request) {
            requests.add(request);
            if (failure != null) throw failure;
            return "받침을 조금 더 크게 써 보세요.";
        }
    }

    @TestConfiguration
    static class StubConfig {
        @Bean
        @Primary
        StubAdviceClient stubAdviceClient() {
            return new StubAdviceClient();
        }
    }

    @Autowired MockMvc baseMvc;
    @Autowired WebApplicationContext ctx;
    @Autowired PracticeAttemptRepository repository;
    @Autowired StubAdviceClient stub;
    @Autowired JsonMapper jsonMapper;
    MockMvc mvc;

    @BeforeEach
    void login() throws Exception {
        repository.deleteAll();
        stub.requests.clear();
        stub.failure = null;
        mvc = TestAuth.signup(baseMvc, ctx).mvc(); // 새 회원이라 하루 횟수도 0부터
    }

    ResultActions save(String text, boolean withImage) throws Exception {
        Map<String, Object> body = PracticeApiTest.attempt(text, 64);
        if (withImage) body.put("image", IMAGE);
        return mvc.perform(post("/api/practice/attempts").contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(body)));
    }

    @Test
    void advice_isSavedWithTheAttempt() throws Exception {
        String res = save("강 물", true)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.advice").value("받침을 조금 더 크게 써 보세요."))
                .andExpect(jsonPath("$.adviceStatus").value("OK"))
                .andExpect(jsonPath("$.cells.length()").value(3))
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(res, "$.id")).longValue();

        mvc.perform(get("/api/practice/attempts/" + id))
                .andExpect(jsonPath("$.advice").value("받침을 조금 더 크게 써 보세요."))
                .andExpect(jsonPath("$.adviceStatus").value("OK"));

        // AI에게는 글씨체 이름, 글, 채점한 글자의 점수와 그림만 간다 (띄어쓰기 칸은 빠짐)
        assertThat(stub.requests).hasSize(1);
        AdviceRequest sent = stub.requests.get(0);
        assertThat(sent.fontLabel()).isEqualTo("고운돋움");
        assertThat(sent.text()).isEqualTo("강 물");
        assertThat(sent.guide()).isTrue();
        assertThat(sent.scores()).extracting(AdviceRequest.CharScore::ch).containsExactly("강", "물");
        assertThat(sent.scores().get(0)).isEqualTo(new AdviceRequest.CharScore("강", 64, 32, 16, 16, false));
        assertThat(sent.png()).startsWith((byte) 0x89, (byte) 'P', (byte) 'N', (byte) 'G');
    }

    @Test
    void failedAdvice_keepsTheAttempt_andDoesNotUseUpTheDailyLimit() throws Exception {
        stub.failure = new AdviceFailedException("조언 서버에 연결하지 못했습니다.");
        for (int i = 0; i < 3; i++) {
            save("가", true)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.advice").isEmpty())
                    .andExpect(jsonPath("$.adviceStatus").value("UNAVAILABLE"))
                    .andExpect(jsonPath("$.totalScore").value(64));
        }
        assertThat(repository.count()).isEqualTo(3);
        assertThat(stub.requests).hasSize(3); // 실패는 횟수에서 빼 주므로 계속 시도된다

        stub.failure = null;
        save("가", true).andExpect(jsonPath("$.adviceStatus").value("OK"));
    }

    @Test
    void unexpectedErrors_alsoKeepTheAttempt() throws Exception {
        stub.failure = new IllegalStateException("boom");
        save("가", true).andExpect(status().isCreated()).andExpect(jsonPath("$.adviceStatus").value("UNAVAILABLE"));
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void noImage_savesWithoutAskingForAdvice() throws Exception {
        save("가", false).andExpect(status().isCreated()).andExpect(jsonPath("$.adviceStatus").value("UNAVAILABLE"));
        assertThat(stub.requests).isEmpty();
    }

    @Test
    void overTheDailyLimit_savesWithoutAdvice() throws Exception {
        save("가", true).andExpect(jsonPath("$.adviceStatus").value("OK"));
        save("가", true).andExpect(jsonPath("$.adviceStatus").value("OK"));
        save("가", true)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.advice").isEmpty())
                .andExpect(jsonPath("$.adviceStatus").value("UNAVAILABLE"));
        assertThat(stub.requests).hasSize(2);
        assertThat(repository.count()).isEqualTo(3);
    }
}
