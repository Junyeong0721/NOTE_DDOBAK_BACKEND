package kr.study.ddobak.practice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kr.study.ddobak.support.TestAuth;
import kr.study.ddobak.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
class PracticeApiTest {

    @Autowired MockMvc baseMvc;
    @Autowired WebApplicationContext ctx;
    @Autowired PracticeAttemptRepository repository;
    @Autowired UserRepository userRepository;
    @Autowired JsonMapper jsonMapper;
    MockMvc mvc; // 가입·로그인된 세션으로 요청
    Long userId;

    @BeforeEach
    void login() throws Exception {
        repository.deleteAll();
        TestAuth.LoggedIn login = TestAuth.signup(baseMvc, ctx);
        mvc = login.mvc();
        userId = login.userId();
    }

    /** 문제 하나: 글자 수만큼 칸과 점수를 채움. 띄어쓰기 칸은 null */
    static Map<String, Object> item(String text, int total) {
        List<Object> cells = new ArrayList<>();
        List<Object> scores = new ArrayList<>();
        text.codePoints().forEach(cp -> {
            String ch = new String(Character.toChars(cp));
            if (ch.equals(" ")) {
                cells.add(null);
                scores.add(null);
                return;
            }
            cells.add(Map.of("ch", ch, "strokes", List.of(Map.of("id", "s1", "points", List.of(List.of(10, 20, 0.5, 0), List.of(30, 40, 0.5, 8))))));
            scores.add(Map.of("total", total, "shape", total / 2.0, "position", total / 4.0, "tilt", total / 4.0, "empty", false, "skipped", false));
        });
        Map<String, Object> item = new HashMap<>();
        item.put("text", text);
        item.put("cells", cells);
        item.put("charScores", scores);
        item.put("score", Map.of("total", total, "shape", total, "position", total, "tilt", total));
        return item;
    }

    /** 한 판: 문제들의 점수는 모두 total */
    static Map<String, Object> round(String stage, int total, String... texts) {
        List<Object> items = new ArrayList<>();
        for (String t : texts) items.add(item(t, total));
        Map<String, Object> body = new HashMap<>();
        body.put("stage", stage);
        body.put("fontId", "gowun-dodum");
        body.put("fontLabel", "고운돋움");
        body.put("guide", true);
        body.put("items", items);
        body.put("totalScore", total);
        body.put("shapeScore", total);
        body.put("positionScore", total);
        body.put("tiltScore", total);
        return body;
    }

    static Map<String, Object> syllables(int total) {
        return round("syllable", total, "강", "물", "밥", "산", "집");
    }

    ResultActions save(MockMvc client, Map<String, Object> body) throws Exception {
        return client.perform(post("/api/practice/attempts").contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(body)));
    }

    long saveOk(Map<String, Object> body) throws Exception {
        String res = save(mvc, body).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.id")).longValue();
    }

    @Test
    void save_returns201_withItemsAndNoAdviceWhenNoImage() throws Exception {
        save(mvc, round("sentence", 82, "오늘 배운 내용 정리", "시험 범위 확인"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.stage").value("sentence"))
                .andExpect(jsonPath("$.text").value("오늘 배운 내용 정리\n시험 범위 확인"))
                .andExpect(jsonPath("$.fontId").value("gowun-dodum"))
                .andExpect(jsonPath("$.guide").value(true))
                .andExpect(jsonPath("$.totalScore").value(82))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].cells.length()").value(11))
                .andExpect(jsonPath("$.items[0].cells[2]").isEmpty())
                .andExpect(jsonPath("$.items[1].score.total").value(82))
                .andExpect(jsonPath("$.advice").isEmpty())
                .andExpect(jsonPath("$.adviceStatus").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void get_returnsStrokes_listOmitsThem() throws Exception {
        long id = saveOk(syllables(70));

        mvc.perform(get("/api/practice/attempts/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("syllable"))
                .andExpect(jsonPath("$.items.length()").value(5))
                .andExpect(jsonPath("$.items[0].text").value("강"))
                .andExpect(jsonPath("$.items[0].cells[0].strokes[0].points[1][0]").value(30))
                .andExpect(jsonPath("$.items[4].charScores[0].shape").value(35.0));
        mvc.perform(get("/api/practice/attempts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].stage").value("syllable"))
                .andExpect(jsonPath("$[0].text").value("강\n물\n밥\n산\n집"))
                .andExpect(jsonPath("$[0].totalScore").value(70))
                .andExpect(jsonPath("$[0].items").doesNotExist());
    }

    @Test
    void list_returnsNewest20() throws Exception {
        long last = 0;
        for (int i = 0; i < 21; i++) last = saveOk(syllables(i));

        mvc.perform(get("/api/practice/attempts"))
                .andExpect(jsonPath("$.length()").value(20))
                .andExpect(jsonPath("$[0].id").value(last))
                .andExpect(jsonPath("$[0].totalScore").value(20))
                .andExpect(jsonPath("$[19].totalScore").value(1));
    }

    @Test
    void best_groupsByStage() throws Exception {
        saveOk(syllables(60));
        saveOk(round("syllable", 85, "가", "나", "다", "라", "마"));
        saveOk(round("word", 40, "강의", "과제", "시험", "출석", "발표"));

        mvc.perform(get("/api/practice/best"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.stage == 'syllable')].best").value(85))
                .andExpect(jsonPath("$[?(@.stage == 'syllable')].count").value(2))
                .andExpect(jsonPath("$[?(@.stage == 'word')].best").value(40))
                .andExpect(jsonPath("$[?(@.stage == 'word')].count").value(1));
    }

    @Test
    void otherAccount_cannotSeeMyAttempts() throws Exception {
        long id = saveOk(syllables(70));
        MockMvc other = TestAuth.signup(baseMvc, ctx).mvc();

        other.perform(get("/api/practice/attempts/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("연습 기록을 찾을 수 없습니다."));
        other.perform(get("/api/practice/attempts")).andExpect(jsonPath("$.length()").value(0));
        other.perform(get("/api/practice/best")).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void withoutLogin_returns401() throws Exception {
        baseMvc.perform(get("/api/practice/attempts")).andExpect(status().isUnauthorized());
        save(baseMvc, syllables(50)).andExpect(status().isUnauthorized());
        assertThat(repository.count()).isZero();
    }

    @Test
    @SuppressWarnings("unchecked")
    void invalidRequests_return400() throws Exception {
        save(mvc, round("jamo", 50, "ㄱ")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("알 수 없는 연습 단계입니다."));

        Map<String, Object> noStage = syllables(50);
        noStage.remove("stage");
        save(mvc, noStage).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("연습 단계를 골라 주세요."));

        save(mvc, round("syllable", 50)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("한 판의 문제는 1~5개여야 합니다."));
        save(mvc, round("syllable", 50, "가", "나", "다", "라", "마", "바")).andExpect(status().isBadRequest());

        save(mvc, round("sentence", 50, "가".repeat(21))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("문제 글은 1~20자여야 합니다."));
        save(mvc, round("sentence", 50, "   ")).andExpect(status().isBadRequest());

        Map<String, Object> wrongCells = round("word", 50, "강의");
        ((Map<String, Object>) ((List<?>) wrongCells.get("items")).get(0)).put("cells", List.of());
        save(mvc, wrongCells).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("쓴 글씨의 칸 수가 글자 수와 다릅니다."));

        Map<String, Object> wrongScores = round("word", 50, "강의");
        ((Map<String, Object>) ((List<?>) wrongScores.get("items")).get(0)).put("charScores", List.of(Map.of("total", 1)));
        save(mvc, wrongScores).andExpect(status().isBadRequest());

        Map<String, Object> noItemScore = round("word", 50, "강의");
        ((Map<String, Object>) ((List<?>) noItemScore.get("items")).get(0)).remove("score");
        save(mvc, noItemScore).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("문제 점수가 없습니다."));

        Map<String, Object> over = syllables(50);
        over.put("totalScore", 101);
        save(mvc, over).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("점수는 0~100이어야 합니다."));

        Map<String, Object> noFont = syllables(50);
        noFont.remove("fontId");
        save(mvc, noFont).andExpect(status().isBadRequest());

        Map<String, Object> jpeg = syllables(50);
        jpeg.put("image", "data:image/jpeg;base64,AAAA");
        save(mvc, jpeg).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("그림 형식이 올바르지 않습니다."));

        Map<String, Object> notPng = syllables(50);
        notPng.put("image", "data:image/png;base64,AAAA");
        save(mvc, notPng).andExpect(status().isBadRequest());

        mvc.perform(get("/api/practice/attempts/abc")).andExpect(status().isBadRequest());
        assertThat(repository.count()).isZero();
    }

    @Test
    void fiveLongSentences_fitInOneRecord() throws Exception {
        String s = "가".repeat(20);
        long id = saveOk(round("sentence", 50, s, s, s, s, s));
        mvc.perform(get("/api/practice/attempts/" + id)).andExpect(jsonPath("$.items[4].cells.length()").value(20));
    }

    @Test
    void deletingTheUser_deletesTheirAttempts() throws Exception {
        saveOk(syllables(70));
        assertThat(repository.count()).isEqualTo(1);

        userRepository.deleteById(userId);

        assertThat(repository.count()).isZero();
    }
}
