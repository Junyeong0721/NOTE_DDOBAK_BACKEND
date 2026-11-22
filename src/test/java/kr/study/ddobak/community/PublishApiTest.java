package kr.study.ddobak.community;

import static kr.study.ddobak.community.CommunityTestSupport.EMPTY;
import static kr.study.ddobak.community.CommunityTestSupport.WRITTEN;
import static kr.study.ddobak.community.CommunityTestSupport.WRITTEN2;
import static kr.study.ddobak.community.CommunityTestSupport.createNote;
import static kr.study.ddobak.community.CommunityTestSupport.publish;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
class PublishApiTest {

    @Autowired MockMvc baseMvc;
    @Autowired WebApplicationContext ctx;
    @Autowired JsonMapper json;
    @Autowired CommunityPostRepository postRepository;
    @Autowired JdbcTemplate jdbc;
    TestAuth.LoggedIn me;
    MockMvc mvc;
    long noteId;

    @BeforeEach
    void setUp() throws Exception {
        me = TestAuth.signup(baseMvc, ctx);
        mvc = me.mvc();
        noteId = createNote(mvc, json, "미적분 2강", WRITTEN);
    }

    String path() {
        return "/api/notes/" + noteId + "/publish";
    }

    @Test
    void notPublished_statusIsFalse() throws Exception {
        mvc.perform(get(path())).andExpect(status().isOk()).andExpect(jsonPath("$.published").value(false));
    }

    @Test
    void emptyNote_cannotBePublished() throws Exception {
        long empty = createNote(mvc, json, "빈 노트", EMPTY);
        publish(mvc, json, empty, "빈 노트", "수학", null, false)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("필기가 있어야 공개할 수 있어요."));
    }

    @Test
    void publish_thenEditInfo_keepsOnePost() throws Exception {
        publish(mvc, json, noteId, "  미적분   2강 ", "수학", "극한 정리", false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(true))
                .andExpect(jsonPath("$.title").value("미적분 2강"))
                .andExpect(jsonPath("$.subject").value("수학"))
                .andExpect(jsonPath("$.intro").value("극한 정리"))
                .andExpect(jsonPath("$.likeCount").value(0))
                .andExpect(jsonPath("$.hidden").value(false))
                .andExpect(jsonPath("$.outdated").value(false));
        publish(mvc, json, noteId, "미적분 2강 (수정)", "수학Ⅱ", "", false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("미적분 2강 (수정)"))
                .andExpect(jsonPath("$.subject").value("수학Ⅱ"))
                .andExpect(jsonPath("$.intro").doesNotExist());
        assertThat(postRepository.findAll().stream().filter(p -> Long.valueOf(noteId).equals(p.getNoteId()))).hasSize(1);
        mvc.perform(get(path())).andExpect(jsonPath("$.title").value("미적분 2강 (수정)"));
    }

    @Test
    void editingTheNote_makesItOutdated_untilRefresh() throws Exception {
        long postId = ((Number) JsonPath.read(publish(mvc, json, noteId, "미적분", "수학", null, false)
                .andReturn().getResponse().getContentAsString(), "$.postId")).longValue();
        Thread.sleep(5);
        mvc.perform(put("/api/notes/" + noteId).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "미적분 2강", "content", WRITTEN2))))
                .andExpect(status().isOk());
        mvc.perform(get(path())).andExpect(jsonPath("$.outdated").value(true));
        assertThat(postRepository.findById(postId).orElseThrow().getContent()).isEqualTo(WRITTEN); // 아직 그대로

        // 정보만 고치면 필기는 그대로
        publish(mvc, json, noteId, "미적분", "수학", null, false).andExpect(jsonPath("$.outdated").value(true));
        publish(mvc, json, noteId, "미적분", "수학", null, true).andExpect(jsonPath("$.outdated").value(false));
        assertThat(postRepository.findById(postId).orElseThrow().getContent()).isEqualTo(WRITTEN2);
    }

    @Test
    void refresh_keepsLikesAndComments() throws Exception {
        long postId = ((Number) JsonPath.read(publish(mvc, json, noteId, "미적분", "수학", null, false)
                .andReturn().getResponse().getContentAsString(), "$.postId")).longValue();
        MockMvc other = TestAuth.signup(baseMvc, ctx).mvc();
        other.perform(put("/api/community/" + postId + "/like")).andExpect(status().isOk());
        other.perform(post("/api/community/" + postId + "/comments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"고마워요\"}")).andExpect(status().isCreated());
        publish(mvc, json, noteId, "미적분", "수학", null, true)
                .andExpect(jsonPath("$.likeCount").value(1))
                .andExpect(jsonPath("$.commentCount").value(1));
    }

    @Test
    void badInfo_isRejected() throws Exception {
        publish(mvc, json, noteId, "가".repeat(101), "수학", null, false)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("제목은 100자까지 쓸 수 있어요."));
        publish(mvc, json, noteId, "  ", "수학", null, false)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("제목을 입력해 주세요."));
        publish(mvc, json, noteId, "미적분", "   ", null, false)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("과목을 입력해 주세요."));
        publish(mvc, json, noteId, "미적분", "가".repeat(31), null, false)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("과목은 30자까지 쓸 수 있어요."));
        publish(mvc, json, noteId, "미적분", "수학", "가".repeat(201), false)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("한 줄 소개는 200자까지 쓸 수 있어요."));
        publish(mvc, json, noteId, "😀".repeat(51), "수학", null, false).andExpect(status().isBadRequest()); // 이모지는 2자
        publish(mvc, json, noteId, "가".repeat(100), "수학", null, false).andExpect(status().isOk());
        mvc.perform(get(path())).andExpect(jsonPath("$.published").value(true));
    }

    @Test
    void unpublish_removesThePost() throws Exception {
        publish(mvc, json, noteId, "미적분", "수학", null, false).andExpect(status().isOk());
        mvc.perform(delete(path())).andExpect(status().isNoContent());
        mvc.perform(get(path())).andExpect(jsonPath("$.published").value(false));
        mvc.perform(delete(path())).andExpect(status().isNoContent()); // 다시 눌러도 괜찮다
    }

    @Test
    void othersNote_is404_andLoginIsRequired() throws Exception {
        MockMvc other = TestAuth.signup(baseMvc, ctx).mvc();
        other.perform(get(path())).andExpect(status().isNotFound());
        publish(other, json, noteId, "남의 노트", "수학", null, false).andExpect(status().isNotFound());
        other.perform(delete(path())).andExpect(status().isNotFound());
        baseMvc.perform(get(path())).andExpect(status().isUnauthorized());
    }

    @Test
    void deletingTheNote_keepsThePost() throws Exception {
        long postId = ((Number) JsonPath.read(publish(mvc, json, noteId, "미적분", "수학", null, false)
                .andReturn().getResponse().getContentAsString(), "$.postId")).longValue();
        mvc.perform(delete("/api/notes/" + noteId)).andExpect(status().isNoContent());
        mvc.perform(get(path())).andExpect(status().isNotFound());
        CommunityPost post = postRepository.findById(postId).orElseThrow();
        assertThat(post.getNoteId()).isNull();
        mvc.perform(get("/api/community/" + postId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(WRITTEN))
                .andExpect(jsonPath("$.mine").value(true));
    }

    @Test
    void deletingTheUser_deletesTheirPosts() throws Exception {
        long postId = ((Number) JsonPath.read(publish(mvc, json, noteId, "미적분", "수학", null, false)
                .andReturn().getResponse().getContentAsString(), "$.postId")).longValue();
        jdbc.update("delete from note where user_id = ?", me.userId());
        jdbc.update("delete from users where id = ?", me.userId());
        assertThat(postRepository.findById(postId)).isEmpty();
    }
}
