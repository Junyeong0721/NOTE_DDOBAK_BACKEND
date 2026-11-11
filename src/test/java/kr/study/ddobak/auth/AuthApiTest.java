package kr.study.ddobak.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import java.util.UUID;
import kr.study.ddobak.support.TestAuth;
import kr.study.ddobak.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {

    static final String CONTENT = "{\"version\":1,\"pages\":[{\"strokes\":[]}]}";

    @Autowired MockMvc mvc;
    @Autowired WebApplicationContext ctx;
    @Autowired JsonMapper jsonMapper;
    @Autowired UserRepository userRepository;

    String email() {
        return "u-" + UUID.randomUUID() + "@Test.kr";
    }

    String json(Object o) {
        return jsonMapper.writeValueAsString(o);
    }

    MvcResult signup(String email, String password, String nickname) throws Exception {
        return mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", password, "nickname", nickname)))).andReturn();
    }

    @Test
    void signup_logsIn_andMeReturnsTheUser_withoutPassword() throws Exception {
        String email = email();
        MvcResult r = signup(email, "abcd1234", " 또박이 ");
        assertThat(r.getResponse().getStatus()).isEqualTo(201);
        MockHttpSession session = (MockHttpSession) r.getRequest().getSession(false);

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email.toLowerCase()))
                .andExpect(jsonPath("$.nickname").value("또박이"))
                .andExpect(jsonPath("$.provider").value("LOCAL"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        assertThat(userRepository.findByEmail(email.toLowerCase()).orElseThrow().getPasswordHash())
                .startsWith("pbkdf2$").doesNotContain("abcd1234");
    }

    @Test
    void signup_sameEmailInAnyCase_returns409() throws Exception {
        String email = email();
        signup(email, "abcd1234", "하나");
        assertThat(signup(email.toUpperCase(), "abcd1234", "둘").getResponse().getStatus()).isEqualTo(409);
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", "abcd1234", "nickname", "셋"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 가입된 이메일입니다."));
    }

    @Test
    void signup_weakPasswordOrBadEmail_returns400() throws Exception {
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email(), "password", "abcdefgh", "nickname", "가"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("비밀번호에 영문과 숫자를 모두 넣어 주세요."));
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email(), "password", "ab1", "nickname", "가"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("비밀번호는 8~64자로 입력해 주세요."));
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "not-an-email", "password", "abcd1234", "nickname", "가"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이메일 형식이 올바르지 않습니다."));
    }

    @Test
    void login_rightPassword_ok_wrongPasswordOrUnknownEmail_sameMessage() throws Exception {
        String email = email();
        signup(email, "abcd1234", "하나");

        MvcResult ok = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email.toUpperCase(), "password", "abcd1234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("하나"))
                .andReturn();
        mvc.perform(get("/api/auth/me").session((MockHttpSession) ok.getRequest().getSession(false))).andExpect(status().isOk());

        for (Map<String, String> bad : new Map[] {
                Map.of("email", email, "password", "wrong1234"), Map.of("email", email(), "password", "abcd1234")}) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json(bad)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 맞지 않습니다."));
        }
    }

    @Test
    void logout_endsTheSession() throws Exception {
        TestAuth.LoggedIn user = TestAuth.signup(mvc, ctx);
        user.mvc().perform(get("/api/auth/me")).andExpect(status().isOk());
        user.mvc().perform(post("/api/auth/logout")).andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").session(user.session())).andExpect(status().isUnauthorized());
    }

    @Test
    void withoutLogin_notesFoldersAndOcrReturn401() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
        mvc.perform(get("/api/notes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/folders")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/ocr/line").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void oneUserCannotSeeOrChangeAnothersNotesAndFolders() throws Exception {
        TestAuth.LoggedIn a = TestAuth.signup(mvc, ctx);
        TestAuth.LoggedIn b = TestAuth.signup(mvc, ctx);

        String note = a.mvc().perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "A의 노트", "content", CONTENT))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long noteId = ((Number) JsonPath.read(note, "$.id")).longValue();
        String folder = a.mvc().perform(post("/api/folders").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "A 폴더", "color", "mint"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long folderId = ((Number) JsonPath.read(folder, "$.id")).longValue();

        b.mvc().perform(get("/api/notes")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        b.mvc().perform(get("/api/folders")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        b.mvc().perform(get("/api/notes/" + noteId)).andExpect(status().isNotFound());
        b.mvc().perform(put("/api/notes/" + noteId).contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("title", "바꿈", "content", CONTENT)))).andExpect(status().isNotFound());
        b.mvc().perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "B", "content", CONTENT, "folderId", folderId))))
                .andExpect(status().isNotFound()); // 남의 폴더에는 못 넣음

        a.mvc().perform(get("/api/notes")).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("A의 노트"));
    }
}
