package kr.study.ddobak.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

/** 테스트용 회원 가입 + 그 세션으로 로그인된 MockMvc */
public final class TestAuth {

    public record LoggedIn(MockMvc mvc, Long userId, MockHttpSession session) {
    }

    private TestAuth() {
    }

    public static LoggedIn signup(MockMvc base, WebApplicationContext ctx) throws Exception {
        String email = "user-" + UUID.randomUUID() + "@test.kr";
        MvcResult result = base.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"abcd1234\",\"nickname\":\"테스트\"}"))
                .andReturn();
        if (result.getResponse().getStatus() != 201) {
            throw new IllegalStateException("가입 실패: " + result.getResponse().getContentAsString());
        }
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        Long userId = ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(ctx)
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .defaultRequest(get("/").session(session))
                .build();
        return new LoggedIn(mvc, userId, session);
    }
}
