package kr.study.ddobak.community;

import static kr.study.ddobak.community.CommunityTestSupport.WRITTEN;
import static kr.study.ddobak.community.CommunityTestSupport.WRITTEN2;
import static kr.study.ddobak.community.CommunityTestSupport.createNote;
import static kr.study.ddobak.community.CommunityTestSupport.publish;
import static kr.study.ddobak.community.CommunityTestSupport.publishNew;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.study.ddobak.note.NoteRepository;
import kr.study.ddobak.support.TestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

/** 두 계정(A가 공개, B가 둘러봄)으로 커뮤니티 API를 본다. 다른 테스트의 글과 섞이지 않게 과목에 고유 꼬리를 붙인다 */
@SpringBootTest
@AutoConfigureMockMvc
class CommunityApiTest {

    @Autowired MockMvc baseMvc;
    @Autowired WebApplicationContext ctx;
    @Autowired JsonMapper json;
    @Autowired CommunityPostRepository postRepository;
    @Autowired NoteRepository noteRepository;
    @Autowired JdbcTemplate jdbc;
    MockMvc a;
    MockMvc b;
    String tag;

    @BeforeEach
    void setUp() throws Exception {
        a = TestAuth.signup(baseMvc, ctx).mvc();
        b = TestAuth.signup(baseMvc, ctx).mvc();
        tag = UUID.randomUUID().toString().substring(0, 8);
    }

    String subj(String name) {
        return name + tag;
    }

    /** 쿼리 값은 MockMvc가 인코딩한다 (k1, v1, k2, v2 ...) */
    ResultActions list(MockMvc who, String... kv) throws Exception {
        var req = get("/api/community");
        for (int i = 0; i < kv.length; i += 2) req.param(kv[i], kv[i + 1]);
        return who.perform(req);
    }

    MockMvc newUser() throws Exception {
        return TestAuth.signup(baseMvc, ctx).mvc();
    }

    @Test
    void list_latestAndPopular_bySubject() throws Exception {
        long p1 = publishNew(a, json, "첫째", subj("수학"));
        long p2 = publishNew(a, json, "둘째", subj("수학"));
        long p3 = publishNew(a, json, "셋째", subj("수학"));
        publishNew(a, json, "영어 노트", subj("영어"));
        b.perform(put("/api/community/" + p1 + "/like")).andExpect(status().isOk());
        newUser().perform(put("/api/community/" + p1 + "/like")).andExpect(status().isOk());
        b.perform(put("/api/community/" + p2 + "/like")).andExpect(status().isOk());

        list(b, "subject", subj("수학"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].id").value(p3))
                .andExpect(jsonPath("$.items[2].id").value(p1))
                .andExpect(jsonPath("$.items[0].content").doesNotExist())
                .andExpect(jsonPath("$.items[0].author").value("테스트"))
                .andExpect(jsonPath("$.items[0].mine").value(false))
                .andExpect(jsonPath("$.hasMore").value(false));
        list(b, "sort", "popular", "subject", subj("수학"))
                .andExpect(jsonPath("$.items[0].id").value(p1))
                .andExpect(jsonPath("$.items[0].likeCount").value(2))
                .andExpect(jsonPath("$.items[0].liked").value(true))
                .andExpect(jsonPath("$.items[1].id").value(p2))
                .andExpect(jsonPath("$.items[2].id").value(p3))
                .andExpect(jsonPath("$.items[2].liked").value(false));
        list(a, "subject", subj("수학")).andExpect(jsonPath("$.items[0].mine").value(true));

        String subjects = b.perform(get("/api/community/subjects")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> rows = JsonPath.read(subjects, "$");
        assertThat(rows.size()).isLessThanOrEqualTo(15);
    }

    @Test
    void list_pagesOf20() throws Exception {
        for (int i = 0; i < 21; i++) publishNew(a, json, "노트 " + i, subj("페이지"));
        list(b, "subject", subj("페이지")).andExpect(jsonPath("$.items.length()").value(20))
                .andExpect(jsonPath("$.hasMore").value(true))
                .andExpect(jsonPath("$.items[0].title").value("노트 20"));
        list(b, "page", "1", "subject", subj("페이지")).andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.hasMore").value(false))
                .andExpect(jsonPath("$.items[0].title").value("노트 0"));
        list(b, "page", "-1").andExpect(status().isBadRequest());
    }

    @Test
    void search_titleSubjectIntroSummary_withLikeCharactersLiteral() throws Exception {
        long withPercent = publishNew(a, json, "100% 정리 " + tag, subj("과학"));
        publishNew(a, json, "1000 정리 " + tag, subj("과학"));
        long underscore = publishNew(a, json, "a_b " + tag, subj("과학"));
        publishNew(a, json, "axb " + tag, subj("과학"));
        long noteId = createNote(a, json, "소개로 찾기", WRITTEN);
        publish(a, json, noteId, "소개로 찾기", subj("과학"), "Photosynthesis 광합성" + tag, false);

        list(b, "q", "0% 정리 " + tag).andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(withPercent));
        list(b, "q", "a_b " + tag).andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(underscore));
        list(b, "q", "photosynthesis 광합성" + tag).andExpect(jsonPath("$.items.length()").value(1)); // 대소문자 무시
        list(b, "q", tag).andExpect(jsonPath("$.items.length()").value(5)); // 과목에도 tag가 있음
        list(b, "q", "가".repeat(51)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("검색어는 50자까지 쓸 수 있어요."));
        list(b, "q", "   ", "subject", subj("과학")).andExpect(jsonPath("$.items.length()").value(5)); // 빈 검색어는 전체
        list(b, "q", "없는말" + tag).andExpect(jsonPath("$.items.length()").value(0));
    }


    @Test
    void detail_showsContentAuthorAndFlags() throws Exception {
        long postId = publishNew(a, json, "미적분", subj("수학"));
        b.perform(get("/api/community/" + postId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(WRITTEN))
                .andExpect(jsonPath("$.author").value("테스트"))
                .andExpect(jsonPath("$.liked").value(false))
                .andExpect(jsonPath("$.mine").value(false))
                .andExpect(jsonPath("$.reported").value(false))
                .andExpect(jsonPath("$.noteId").doesNotExist()); // 남의 원본 노트 번호는 안 보여 준다
        a.perform(get("/api/community/" + postId)).andExpect(jsonPath("$.mine").value(true))
                .andExpect(jsonPath("$.noteId").isNumber());
        b.perform(get("/api/community/999999999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("공개된 노트를 찾을 수 없어요."));
    }

    @Test
    void like_isIdempotent_andNotOnMyOwn() throws Exception {
        long postId = publishNew(a, json, "미적분", subj("수학"));
        String url = "/api/community/" + postId + "/like";
        b.perform(put(url)).andExpect(jsonPath("$.liked").value(true)).andExpect(jsonPath("$.likeCount").value(1));
        b.perform(put(url)).andExpect(jsonPath("$.liked").value(true)).andExpect(jsonPath("$.likeCount").value(1));
        b.perform(get("/api/community/" + postId)).andExpect(jsonPath("$.liked").value(true));
        b.perform(delete(url)).andExpect(jsonPath("$.liked").value(false)).andExpect(jsonPath("$.likeCount").value(0));
        b.perform(delete(url)).andExpect(jsonPath("$.likeCount").value(0));
        a.perform(put(url)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("내 노트에는 좋아요를 누를 수 없어요."));
        Integer rows = jdbc.queryForObject("select count(*) from post_like where post_id = ?", Integer.class, postId);
        assertThat(rows).isZero();
    }

    @Test
    void copy_makesMyOwnNote_thatDoesNotChangeLater() throws Exception {
        long noteId = createNote(a, json, "미적분", WRITTEN);
        long postId = ((Number) JsonPath.read(publish(a, json, noteId, "미적분 정리", subj("수학"), null, false)
                .andReturn().getResponse().getContentAsString(), "$.postId")).longValue();
        jdbc.update("update community_post set summary = ? where id = ?", "요약 한 줄", postId);

        String res = b.perform(post("/api/community/" + postId + "/copy")).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long copied = ((Number) JsonPath.read(res, "$.noteId")).longValue();
        b.perform(get("/api/notes/" + copied)).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("미적분 정리 (가져옴)"))
                .andExpect(jsonPath("$.content").value(WRITTEN))
                .andExpect(jsonPath("$.summary").doesNotExist());
        a.perform(get("/api/notes/" + noteId + "/publish")).andExpect(jsonPath("$.copyCount").value(1));

        Thread.sleep(5);
        a.perform(put("/api/notes/" + noteId).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("title", "미적분", "content", WRITTEN2)))).andExpect(status().isOk());
        publish(a, json, noteId, "미적분 정리", subj("수학"), null, true).andExpect(status().isOk());
        b.perform(get("/api/community/" + postId)).andExpect(jsonPath("$.content").value(WRITTEN2));
        b.perform(get("/api/notes/" + copied)).andExpect(jsonPath("$.content").value(WRITTEN)); // 가져온 노트는 그대로
        a.perform(get("/api/notes/" + copied)).andExpect(status().isNotFound()); // B의 노트
    }

    @Test
    void comments_writeListDelete_withPermissions() throws Exception {
        long postId = publishNew(a, json, "미적분", subj("수학"));
        MockMvc c = newUser();
        String url = "/api/community/" + postId + "/comments";
        long bComment = commentId(b.perform(post(url).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"  잘 봤어요  \"}")).andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("잘 봤어요"))
                .andExpect(jsonPath("$.mine").value(true)));
        long cComment = commentId(c.perform(post(url).contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"" + "가".repeat(500) + "\"}")).andExpect(status().isCreated()));
        b.perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"" + "가".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("댓글은 500자까지 쓸 수 있어요."));
        b.perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"   \"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("댓글 내용을 입력해 주세요."));

        b.perform(get(url)).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(bComment))
                .andExpect(jsonPath("$[0].canDelete").value(true))
                .andExpect(jsonPath("$[1].canDelete").value(false));
        a.perform(get(url)).andExpect(jsonPath("$[1].canDelete").value(true)); // 글 주인은 다 지울 수 있다
        b.perform(get("/api/community/" + postId)).andExpect(jsonPath("$.commentCount").value(2));

        b.perform(delete(url + "/" + cComment)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("내 댓글이나 내 노트에 달린 댓글만 지울 수 있어요."));
        a.perform(delete(url + "/" + cComment)).andExpect(status().isNoContent());
        b.perform(delete(url + "/" + bComment)).andExpect(status().isNoContent());
        b.perform(delete(url + "/" + bComment)).andExpect(status().isNotFound());
        b.perform(get("/api/community/" + postId)).andExpect(jsonPath("$.commentCount").value(0));

        long other = publishNew(a, json, "다른 글", subj("수학"));
        long onOther = commentId(b.perform(post("/api/community/" + other + "/comments")
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"다른 글 댓글\"}")));
        b.perform(delete(url + "/" + onOther)).andExpect(status().isNotFound()); // 다른 글 주소로는 못 지움
    }

    long commentId(ResultActions r) throws Exception {
        return ((Number) JsonPath.read(r.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test
    void report_once_andThreeHide_forEveryoneButTheOwner() throws Exception {
        long postId = publishNew(a, json, "숨겨질 글", subj("신고"));
        String url = "/api/community/" + postId + "/report";
        String body = "{\"reason\":\"spam\"}";
        b.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNoContent());
        b.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 신고한 노트예요."));
        b.perform(get("/api/community/" + postId)).andExpect(jsonPath("$.reported").value(true));
        a.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("내 노트는 신고할 수 없어요."));
        MockMvc c = newUser();
        c.perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"hate\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("신고 이유를 골라 주세요."));
        c.perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"inappropriate\"}"))
                .andExpect(status().isNoContent());
        b.perform(get("/api/community/" + postId)).andExpect(status().isOk()); // 2명까지는 보인다
        newUser().perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"other\"}"))
                .andExpect(status().isNoContent());

        String base = "/api/community/" + postId;
        b.perform(get(base)).andExpect(status().isNotFound());
        b.perform(get(base + "/comments")).andExpect(status().isNotFound());
        b.perform(put(base + "/like")).andExpect(status().isNotFound());
        b.perform(post(base + "/copy")).andExpect(status().isNotFound());
        list(b, "subject", subj("신고")).andExpect(jsonPath("$.items.length()").value(0));
        list(b, "q", "숨겨질 글").andExpect(jsonPath("$.items[?(@.id == " + postId + ")]").isEmpty());
        String subjects = b.perform(get("/api/community/subjects")).andReturn().getResponse().getContentAsString();
        assertThat(subjects).doesNotContain(subj("신고"));

        a.perform(get(base)).andExpect(status().isOk()).andExpect(jsonPath("$.hidden").value(true));
        list(a, "subject", subj("신고")).andExpect(jsonPath("$.items[0].hidden").value(true));
        long noteId = postRepository.findById(postId).orElseThrow().getNoteId();
        a.perform(get("/api/notes/" + noteId + "/publish")).andExpect(jsonPath("$.hidden").value(true));
    }

    @Test
    void loginIsRequired() throws Exception {
        baseMvc.perform(get("/api/community")).andExpect(status().isUnauthorized());
        baseMvc.perform(get("/api/community/subjects")).andExpect(status().isUnauthorized());
        baseMvc.perform(put("/api/community/1/like")).andExpect(status().isUnauthorized());
    }
}
