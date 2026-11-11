package kr.study.ddobak.ocr;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Base64;
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

@SpringBootTest
@AutoConfigureMockMvc
class OcrApiTest {

    /** 1x1 흰색 PNG */
    static final String ONE_PIXEL_PNG =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==";

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

    String body(String imageBase64, int width, int height) {
        return jsonMapper.writeValueAsString(Map.of("imageBase64", imageBase64, "width", width, "height", height));
    }

    @Test
    void recognize_validPng_returnsFakeResult() throws Exception {
        mvc.perform(post("/api/ocr/line").contentType(MediaType.APPLICATION_JSON)
                        .content(body(ONE_PIXEL_PNG, 1, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.engine").value("fake"))
                .andExpect(jsonPath("$.text").value(""))
                .andExpect(jsonPath("$.chars").isArray())
                .andExpect(jsonPath("$.chars.length()").value(0));
    }

    @Test
    void recognize_notBase64_returns400() throws Exception {
        mvc.perform(post("/api/ocr/line").contentType(MediaType.APPLICATION_JSON)
                        .content(body("@@@", 10, 10)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미지 형식이 올바르지 않습니다."));
    }

    @Test
    void recognize_notPng_returns400() throws Exception {
        mvc.perform(post("/api/ocr/line").contentType(MediaType.APPLICATION_JSON)
                        .content(body("aGVsbG8=", 10, 10))) // "hello"
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미지 형식이 올바르지 않습니다."));
    }

    @Test
    void recognize_tooLarge_returns400() throws Exception {
        byte[] big = new byte[OcrController.MAX_IMAGE_BYTES + 1];
        byte[] signature = Base64.getDecoder().decode(ONE_PIXEL_PNG);
        System.arraycopy(signature, 0, big, 0, 8);
        Arrays.fill(big, 8, big.length, (byte) 1);

        mvc.perform(post("/api/ocr/line").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Base64.getEncoder().encodeToString(big), 100, 100)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미지가 너무 큽니다."));
    }

    @Test
    void recognize_zeroWidth_returns400() throws Exception {
        mvc.perform(post("/api/ocr/line").contentType(MediaType.APPLICATION_JSON)
                        .content(body(ONE_PIXEL_PNG, 0, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미지 크기가 올바르지 않습니다."));
    }
}
