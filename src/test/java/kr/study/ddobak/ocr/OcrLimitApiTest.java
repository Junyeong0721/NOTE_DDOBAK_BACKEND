package kr.study.ddobak.ocr;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import kr.study.ddobak.support.TestAuth;
import tools.jackson.databind.json.JsonMapper;

/** 하루 한도를 1번으로 줄여서 두 번째 요청이 429로 막히는지 본다. */
@SpringBootTest(properties = "ddobak.ocr.daily-limit=1")
@AutoConfigureMockMvc
class OcrLimitApiTest {

    @Autowired MockMvc baseMvc;
    @Autowired WebApplicationContext ctx;
    MockMvc mvc; // 가입·로그인된 세션으로 요청
    Long userId;

    @BeforeEach
    void login() throws Exception {
        TestAuth.LoggedIn login = TestAuth.signup(baseMvc, ctx);
        mvc = login.mvc();
        userId = login.userId();
    }
    @Autowired JsonMapper jsonMapper;

    @Test
    void recognize_overDailyLimit_returns429() throws Exception {
        String body = jsonMapper.writeValueAsString(
                Map.of("imageBase64", OcrApiTest.ONE_PIXEL_PNG, "width", 1, "height", 1));

        mvc.perform(post("/api/ocr/line").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mvc.perform(post("/api/ocr/line").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("오늘 글자 인식 횟수(1번)를 다 썼습니다. 내일 다시 시도해 주세요."));
    }
}
