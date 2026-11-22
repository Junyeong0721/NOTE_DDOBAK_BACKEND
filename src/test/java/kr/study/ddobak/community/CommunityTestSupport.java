package kr.study.ddobak.community;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

/** 커뮤니티 테스트에서 같이 쓰는 노트 만들기·공개하기 */
final class CommunityTestSupport {

    static final String WRITTEN = "{\"version\":1,\"pages\":[{\"strokes\":[{\"points\":[[1,2],[3,4]]}]}]}";
    static final String WRITTEN2 = "{\"version\":1,\"pages\":[{\"strokes\":[{\"points\":[[5,6],[7,8]]}]},{\"strokes\":[]}]}";
    static final String EMPTY = "{\"version\":1,\"pages\":[{\"strokes\":[]}]}";

    private CommunityTestSupport() {
    }

    static long createNote(MockMvc mvc, JsonMapper json, String title, String content) throws Exception {
        String res = mvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", title, "content", content))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.id")).longValue();
    }

    static ResultActions publish(MockMvc mvc, JsonMapper json, long noteId, String title, String subject, String intro,
                                 boolean refresh) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("subject", subject);
        body.put("intro", intro);
        body.put("refreshContent", refresh);
        return mvc.perform(put("/api/notes/" + noteId + "/publish").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    static long publishNew(MockMvc mvc, JsonMapper json, String title, String subject) throws Exception {
        long noteId = createNote(mvc, json, title, WRITTEN);
        String res = publish(mvc, json, noteId, title, subject, null, false)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.postId")).longValue();
    }
}
