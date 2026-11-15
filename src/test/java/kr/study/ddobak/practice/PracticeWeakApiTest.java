package kr.study.ddobak.practice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kr.study.ddobak.support.TestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
class PracticeWeakApiTest {

    @Autowired MockMvc baseMvc;
    @Autowired WebApplicationContext ctx;
    @Autowired PracticeAttemptRepository repository;
    @Autowired JsonMapper jsonMapper;
    @Autowired JdbcTemplate jdbc;
    MockMvc mvc;

    @BeforeEach
    void login() throws Exception {
        repository.deleteAll();
        mvc = TestAuth.signup(baseMvc, ctx).mvc();
    }

    /** 글자 한 판: scores[i]는 i번째 글자 점수, null이면 안 쓴 칸. 항목은 모양이 가장 약하게 */
    void play(String text, Integer... scores) throws Exception {
        List<Object> cells = new ArrayList<>();
        List<Object> charScores = new ArrayList<>();
        int[] chars = text.codePoints().toArray();
        for (int i = 0; i < chars.length; i++) {
            String ch = new String(Character.toChars(chars[i]));
            cells.add(Map.of("ch", ch, "strokes", List.of()));
            Integer t = scores[i];
            charScores.add(t == null
                    ? Map.of("total", 0, "shape", 0, "position", 0, "tilt", 0, "empty", true, "skipped", false)
                    : Map.of("total", t, "shape", t * 0.4, "position", t * 0.3, "tilt", t * 0.3, "empty", false, "skipped", false));
        }
        Map<String, Object> item = new HashMap<>();
        item.put("text", text);
        item.put("cells", cells);
        item.put("charScores", charScores);
        item.put("score", Map.of("total", 50, "shape", 50, "position", 50, "tilt", 50));
        Map<String, Object> body = new HashMap<>(Map.of("stage", "word", "fontId", "gowun-dodum", "guide", true,
                "items", List.of(item), "totalScore", 50, "shapeScore", 50, "positionScore", 50, "tiltScore", 50));
        mvc.perform(post("/api/practice/attempts").contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    @Test
    void lowScoringCharacters_areWeak_lowestFirst() throws Exception {
        play("강의", 50, 90);
        play("강물", 70, 60);
        play("과제", 95, 79);

        mvc.perform(get("/api/practice/weak"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].ch").value("강"))
                .andExpect(jsonPath("$[0].average").value(60))
                .andExpect(jsonPath("$[0].count").value(2))
                .andExpect(jsonPath("$[0].weakest").value("shape"))
                .andExpect(jsonPath("$[1].ch").value("물"))
                .andExpect(jsonPath("$[2].ch").value("제"))
                .andExpect(jsonPath("$[2].average").value(79));
    }

    @Test
    void unwrittenCells_doNotCountAsWeak() throws Exception {
        play("강의", null, null);
        mvc.perform(get("/api/practice/weak")).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void recentGoodScores_meanTheCharacterIsOvercome() throws Exception {
        play("닭", 30);
        play("닭", 40);
        play("닭", 85);
        play("닭", 88);
        mvc.perform(get("/api/practice/weak")).andExpect(jsonPath("$[0].ch").value("닭")); // 최근 3번 평균 71

        play("닭", 90); // 최근 3번 평균 88 → 극복
        mvc.perform(get("/api/practice/weak")).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void onlyTheLast30Days_count() throws Exception {
        play("값", 20);
        jdbc.update("update practice_attempt set created_at = DATEADD('DAY', -31, CURRENT_TIMESTAMP)");
        play("읽", 40);
        mvc.perform(get("/api/practice/weak"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ch").value("읽"));
    }

    @Test
    void otherAccounts_doNotMix() throws Exception {
        play("강", 20);
        MockMvc other = TestAuth.signup(baseMvc, ctx).mvc();
        other.perform(get("/api/practice/weak")).andExpect(jsonPath("$.length()").value(0));
        baseMvc.perform(get("/api/practice/weak")).andExpect(status().isUnauthorized());
    }

    @Test
    void weakStage_canBeSaved() throws Exception {
        Map<String, Object> body = PracticeApiTest.round("weak", 60, "강", "닭", "값", "읽", "물");
        mvc.perform(post("/api/practice/attempts").contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stage").value("weak"));
    }
}
