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

    /** text 글자 수만큼 칸과 점수를 채운 요청. 띄어쓰기 칸은 null */
    static Map<String, Object> attempt(String text, int total) {
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
        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        body.put("fontId", "gowun-dodum");
        body.put("fontLabel", "고운돋움");
        body.put("guide", true);
        body.put("cells", cells);
        body.put("charScores", scores);
        body.put("totalScore", total);
        body.put("shapeScore", total);
        body.put("positionScore", total);
        body.put("tiltScore", total);
        return body;
    }

    ResultActions save(MockMvc client, Map<String, Object> body) throws Exception {
        return client.perform(post("/api/practice/attempts").contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(body)));
    }

    long saveOk(String text, int total) throws Exception {
        String res = save(mvc, attempt(text, total)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.id")).longValue();
    }

    @Test
    void save_returns201_withScoresAndNoAdviceWhenNoImage() throws Exception {
        Map<String, Object> body = attempt("가나 다", 82);
        body.put("setId", "syllable-1");
        save(mvc, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.setId").value("syllable-1"))
                .andExpect(jsonPath("$.text").value("가나 다"))
                .andExpect(jsonPath("$.fontId").value("gowun-dodum"))
                .andExpect(jsonPath("$.guide").value(true))
                .andExpect(jsonPath("$.totalScore").value(82))
                .andExpect(jsonPath("$.cells.length()").value(4))
                .andExpect(jsonPath("$.cells[2]").isEmpty())
                .andExpect(jsonPath("$.charScores[0].total").value(82))
                .andExpect(jsonPath("$.advice").isEmpty())
                .andExpect(jsonPath("$.adviceStatus").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void get_returnsStrokes_listOmitsThem() throws Exception {
        long id = saveOk("가나", 70);

        mvc.perform(get("/api/practice/attempts/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("가나"))
                .andExpect(jsonPath("$.cells[0].ch").value("가"))
                .andExpect(jsonPath("$.cells[0].strokes[0].points[1][0]").value(30))
                .andExpect(jsonPath("$.charScores[1].shape").value(35.0));
        mvc.perform(get("/api/practice/attempts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].totalScore").value(70))
                .andExpect(jsonPath("$[0].cells").doesNotExist());
    }

    @Test
    void list_returnsNewest20() throws Exception {
        long last = 0;
        for (int i = 0; i < 21; i++) last = saveOk("가", i);

        mvc.perform(get("/api/practice/attempts"))
                .andExpect(jsonPath("$.length()").value(20))
                .andExpect(jsonPath("$[0].id").value(last))
                .andExpect(jsonPath("$[0].totalScore").value(20))
                .andExpect(jsonPath("$[19].totalScore").value(1));
    }

    @Test
    void best_groupsBySameText() throws Exception {
        saveOk("강물밥산집", 60);
        saveOk("강물밥산집", 85);
        saveOk("가나", 40);

        mvc.perform(get("/api/practice/best"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.text == '강물밥산집')].best").value(85))
                .andExpect(jsonPath("$[?(@.text == '강물밥산집')].count").value(2))
                .andExpect(jsonPath("$[?(@.text == '가나')].best").value(40));
    }

    @Test
    void otherAccount_cannotSeeMyAttempts() throws Exception {
        long id = saveOk("가나", 70);
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
        save(baseMvc, attempt("가", 50)).andExpect(status().isUnauthorized());
        assertThat(repository.count()).isZero();
    }

    @Test
    void invalidRequests_return400() throws Exception {
        Map<String, Object> blank = attempt("가", 50);
        blank.put("text", "   ");
        save(mvc, blank).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("연습할 글을 입력해 주세요."));

        save(mvc, attempt("가".repeat(31), 50)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("연습할 글은 30자 이하로 입력해 주세요."));

        Map<String, Object> wrongCells = attempt("가나", 50);
        wrongCells.put("cells", List.of());
        save(mvc, wrongCells).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("쓴 글씨의 칸 수가 글자 수와 다릅니다."));

        Map<String, Object> wrongScores = attempt("가나", 50);
        wrongScores.put("charScores", List.of(Map.of("total", 1)));
        save(mvc, wrongScores).andExpect(status().isBadRequest());

        Map<String, Object> over = attempt("가", 50);
        over.put("totalScore", 101);
        save(mvc, over).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("점수는 0~100이어야 합니다."));

        Map<String, Object> noFont = attempt("가", 50);
        noFont.remove("fontId");
        save(mvc, noFont).andExpect(status().isBadRequest());

        Map<String, Object> jpeg = attempt("가", 50);
        jpeg.put("image", "data:image/jpeg;base64,AAAA");
        save(mvc, jpeg).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("그림 형식이 올바르지 않습니다."));

        Map<String, Object> notPng = attempt("가", 50);
        notPng.put("image", "data:image/png;base64,AAAA");
        save(mvc, notPng).andExpect(status().isBadRequest());

        mvc.perform(get("/api/practice/attempts/abc")).andExpect(status().isBadRequest());
        assertThat(repository.count()).isZero();
    }

    @Test
    void thirtyCharacters_areAccepted() throws Exception {
        saveOk("가".repeat(30), 50);
    }

    @Test
    void deletingTheUser_deletesTheirAttempts() throws Exception {
        saveOk("가나", 70);
        assertThat(repository.count()).isEqualTo(1);

        userRepository.deleteById(userId);

        assertThat(repository.count()).isZero();
    }
}
