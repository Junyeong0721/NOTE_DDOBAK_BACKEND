package kr.study.ddobak.note;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.HashMap;
import java.util.Map;
import kr.study.ddobak.folder.Folder;
import kr.study.ddobak.folder.FolderColor;
import kr.study.ddobak.folder.FolderRepository;
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
class NoteApiTest {

    static final String EMPTY_CONTENT = "{\"version\":1,\"pages\":[{\"strokes\":[]}]}";

    @Autowired MockMvc baseMvc;
    @Autowired WebApplicationContext ctx;
    MockMvc mvc; // 가입·로그인된 세션으로 요청
    Long userId;
    @Autowired NoteRepository repository;
    @Autowired FolderRepository folderRepository;
    @Autowired JsonMapper jsonMapper;

    @BeforeEach
    void clean() throws Exception {
        TestAuth.LoggedIn login = TestAuth.signup(baseMvc, ctx);
        mvc = login.mvc();
        userId = login.userId();
        repository.deleteAll();
    }

    String body(String title, String content) {
        return jsonMapper.writeValueAsString(Map.of("title", title, "content", content));
    }

    long createNote(String title, String content) throws Exception {
        String res = mvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON)
                        .content(body(title, content)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.id")).longValue();
    }

    // ---------- 생성 ----------

    @Test
    void create_returns201WithSavedNote() throws Exception {
        mvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON)
                        .content(body("전공 강의 1주차", EMPTY_CONTENT)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("전공 강의 1주차"))
                .andExpect(jsonPath("$.content").value(EMPTY_CONTENT))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void create_blankTitle_returns400() throws Exception {
        mvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON)
                        .content(body("  ", EMPTY_CONTENT)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("제목을 입력해 주세요."));
    }

    @Test
    void create_title100Chars_isAccepted() throws Exception {
        mvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON)
                        .content(body("가".repeat(100), EMPTY_CONTENT)))
                .andExpect(status().isCreated());
    }

    @Test
    void create_title101Chars_returns400() throws Exception {
        mvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON)
                        .content(body("가".repeat(101), EMPTY_CONTENT)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("제목은 100자 이하로 입력해 주세요."));
    }

    @Test
    void create_nonJsonContent_returns400() throws Exception {
        mvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON)
                        .content(body("메모", "hello")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("필기 데이터 형식이 올바르지 않습니다."));
    }

    @Test
    void create_contentNotObject_returns400() throws Exception {
        mvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON)
                        .content(body("메모", "123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("필기 데이터 형식이 올바르지 않습니다."));
    }

    @Test
    void create_contentWithoutPages_returns400() throws Exception {
        mvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON)
                        .content(body("메모", "{\"version\":1}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("필기 데이터 형식이 올바르지 않습니다."));
    }

    @Test
    void create_contentWithoutVersion_returns400() throws Exception {
        mvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON)
                        .content(body("메모", "{\"pages\":[]}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("필기 데이터 형식이 올바르지 않습니다."));
    }

    @Test
    void create_contentWithPageSize_isAccepted() throws Exception {
        String content = "{\"version\":1,\"page\":{\"width\":1000,\"height\":1414},\"pages\":[{\"strokes\":[]}]}";
        mvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON)
                        .content(body("메모", content)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value(content));
    }

    @Test
    void create_malformedBody_returns400() throws Exception {
        mvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"메모\", "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("요청 형식이 올바르지 않습니다."));
    }

    @Test
    void create_largeContent_savedWithoutTruncation() throws Exception {
        StringBuilder sb = new StringBuilder("{\"version\":1,\"pages\":[{\"strokes\":[");
        int i = 0;
        while (sb.length() < 1_000_000) {
            if (i++ > 0) sb.append(',');
            sb.append("{\"points\":[[12.5,30.25,0.5],[13.0,31.75,0.6]]}");
        }
        sb.append("]}]}");
        String big = sb.toString();

        long id = createNote("획이 아주 많은 노트", big);

        mvc.perform(get("/api/notes/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("획이 아주 많은 노트"))
                .andExpect(jsonPath("$.content").value(big));
    }

    // ---------- 단건 조회 ----------

    @Test
    void get_returnsSavedNote() throws Exception {
        long id = createNote("자료구조 3주차", EMPTY_CONTENT);

        mvc.perform(get("/api/notes/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("자료구조 3주차"))
                .andExpect(jsonPath("$.content").value(EMPTY_CONTENT));
    }

    @Test
    void get_unknownId_returns404() throws Exception {
        mvc.perform(get("/api/notes/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("노트를 찾을 수 없습니다. id=999999"));
    }

    @Test
    void get_nonNumericId_returns400() throws Exception {
        mvc.perform(get("/api/notes/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("잘못된 번호입니다."));
    }

    // ---------- 목록 ----------

    @Test
    void list_returnsSummariesNewestFirst() throws Exception {
        long first = createNote("첫 노트", EMPTY_CONTENT);
        createNote("둘째 노트", EMPTY_CONTENT);
        mvc.perform(put("/api/notes/" + first).contentType(MediaType.APPLICATION_JSON)
                        .content(body("첫 노트(수정)", EMPTY_CONTENT)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/notes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("첫 노트(수정)"))
                .andExpect(jsonPath("$[1].title").value("둘째 노트"))
                .andExpect(jsonPath("$[0].content").doesNotExist());
    }

    // ---------- 수정 ----------

    @Test
    void update_changesTitleAndContent() throws Exception {
        long id = createNote("원래 제목", EMPTY_CONTENT);
        String newContent = "{\"version\":1,\"pages\":[{\"strokes\":[{\"points\":[[1,2,0.5]]}]}]}";

        mvc.perform(put("/api/notes/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(body("바뀐 제목", newContent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("바뀐 제목"));

        mvc.perform(get("/api/notes/" + id))
                .andExpect(jsonPath("$.title").value("바뀐 제목"))
                .andExpect(jsonPath("$.content").value(newContent));
    }

    @Test
    void update_unknownId_returns404() throws Exception {
        mvc.perform(put("/api/notes/999999").contentType(MediaType.APPLICATION_JSON)
                        .content(body("제목", EMPTY_CONTENT)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("노트를 찾을 수 없습니다. id=999999"));
    }

    @Test
    void update_nonJsonContent_returns400() throws Exception {
        long id = createNote("메모", EMPTY_CONTENT);

        mvc.perform(put("/api/notes/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(body("메모", "hello")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("필기 데이터 형식이 올바르지 않습니다."));
    }

    // ---------- 삭제 ----------

    @Test
    void delete_returns204ThenGetReturns404() throws Exception {
        long id = createNote("지울 노트", EMPTY_CONTENT);

        mvc.perform(delete("/api/notes/" + id))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/notes/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_unknownId_returns404() throws Exception {
        mvc.perform(delete("/api/notes/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("노트를 찾을 수 없습니다. id=999999"));
    }

    // ---------- 폴더·썸네일 ----------

    static final String THUMB = "data:image/png;base64,iVBORw0KGgo=";

    String json(Map<String, Object> map) {
        return jsonMapper.writeValueAsString(map);
    }

    Map<String, Object> noteMap(String title, Long folderId, String thumbnail) {
        Map<String, Object> m = new HashMap<>();
        m.put("title", title);
        m.put("content", EMPTY_CONTENT);
        m.put("folderId", folderId);
        m.put("thumbnail", thumbnail);
        return m;
    }

    long folder(String name) {
        return folderRepository.save(new Folder(userId, name, FolderColor.mint)).getId();
    }

    long createWith(String title, Long folderId) throws Exception {
        String res = mvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON)
                        .content(json(noteMap(title, folderId, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.id")).longValue();
    }

    @Test
    void create_withFolder_returnsFolderId() throws Exception {
        long folderId = folder("자료구조");

        mvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON).content(json(noteMap("트리", folderId, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.folderId").value(folderId));
    }

    @Test
    void create_unknownFolder_returns404() throws Exception {
        mvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON).content(json(noteMap("트리", 999999L, null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("폴더를 찾을 수 없습니다. id=999999"));
    }

    @Test
    void update_keepsFolder() throws Exception {
        long folderId = folder("자료구조");
        long id = createWith("트리", folderId);

        mvc.perform(put("/api/notes/" + id).contentType(MediaType.APPLICATION_JSON).content(json(noteMap("트리(수정)", null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderId").value(folderId));
    }

    @Test
    void update_withThumbnail_listShowsIt() throws Exception {
        long id = createWith("트리", null);

        mvc.perform(put("/api/notes/" + id).contentType(MediaType.APPLICATION_JSON).content(json(noteMap("트리", null, THUMB))))
                .andExpect(status().isOk());

        mvc.perform(get("/api/notes"))
                .andExpect(jsonPath("$[0].thumbnail").value(THUMB))
                .andExpect(jsonPath("$[0].content").doesNotExist());
    }

    @Test
    void update_nullThumbnail_keepsPrevious() throws Exception {
        long id = createWith("트리", null);
        mvc.perform(put("/api/notes/" + id).contentType(MediaType.APPLICATION_JSON).content(json(noteMap("트리", null, THUMB))));

        mvc.perform(put("/api/notes/" + id).contentType(MediaType.APPLICATION_JSON).content(json(noteMap("트리2", null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.thumbnail").value(THUMB));
    }

    @Test
    void update_invalidThumbnail_returns400() throws Exception {
        long id = createWith("트리", null);

        mvc.perform(put("/api/notes/" + id).contentType(MediaType.APPLICATION_JSON).content(json(noteMap("트리", null, "http://example.com/a.png"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("썸네일 형식이 올바르지 않습니다."));
    }

    @Test
    void moveToFolder_changesFolder() throws Exception {
        long folderId = folder("운영체제");
        long id = createWith("스케줄링", null);

        mvc.perform(patch("/api/notes/" + id + "/folder").contentType(MediaType.APPLICATION_JSON).content("{\"folderId\":" + folderId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderId").value(folderId))
                .andExpect(jsonPath("$.content").doesNotExist());
    }

    @Test
    void moveToFolder_null_unfiles() throws Exception {
        long id = createWith("스케줄링", folder("운영체제"));

        mvc.perform(patch("/api/notes/" + id + "/folder").contentType(MediaType.APPLICATION_JSON).content("{\"folderId\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderId").doesNotExist());
    }

    @Test
    void moveToFolder_unknownFolder_returns404() throws Exception {
        long id = createWith("스케줄링", null);

        mvc.perform(patch("/api/notes/" + id + "/folder").contentType(MediaType.APPLICATION_JSON).content("{\"folderId\":999999}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("폴더를 찾을 수 없습니다. id=999999"));
    }

    @Test
    void moveToFolder_unknownNote_returns404() throws Exception {
        mvc.perform(patch("/api/notes/999999/folder").contentType(MediaType.APPLICATION_JSON).content("{\"folderId\":null}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("노트를 찾을 수 없습니다. id=999999"));
    }
}
