package kr.study.ddobak.folder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.HashMap;
import java.util.Map;
import kr.study.ddobak.note.NoteRepository;
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
class FolderApiTest {

    static final String EMPTY_CONTENT = "{\"version\":1,\"pages\":[{\"strokes\":[]}]}";

    @Autowired MockMvc baseMvc;
    @Autowired WebApplicationContext ctx;
    MockMvc mvc; // 가입·로그인된 세션으로 요청
    Long userId;
    @Autowired FolderRepository folderRepository;
    @Autowired NoteRepository noteRepository;
    @Autowired JsonMapper jsonMapper;

    @BeforeEach
    void clean() throws Exception {
        TestAuth.LoggedIn login = TestAuth.signup(baseMvc, ctx);
        mvc = login.mvc();
        userId = login.userId();
        noteRepository.deleteAll();
        folderRepository.deleteAll();
    }

    String folderBody(String name, String color) {
        return jsonMapper.writeValueAsString(Map.of("name", name, "color", color));
    }

    long createFolder(String name, String color) throws Exception {
        String res = mvc.perform(post("/api/folders").contentType(MediaType.APPLICATION_JSON).content(folderBody(name, color)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.id")).longValue();
    }

    long createNote(String title, Long folderId) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("content", EMPTY_CONTENT);
        body.put("folderId", folderId);
        String res = mvc.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.id")).longValue();
    }

    @Test
    void create_returns201() throws Exception {
        mvc.perform(post("/api/folders").contentType(MediaType.APPLICATION_JSON).content(folderBody("자료구조", "mint")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("자료구조"))
                .andExpect(jsonPath("$.color").value("mint"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void list_inCreatedOrder() throws Exception {
        createFolder("자료구조", "mint");
        createFolder("운영체제", "sky");

        mvc.perform(get("/api/folders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("자료구조"))
                .andExpect(jsonPath("$[1].name").value("운영체제"));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        mvc.perform(post("/api/folders").contentType(MediaType.APPLICATION_JSON).content(folderBody("  ", "mint")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("폴더 이름을 입력해 주세요."));
    }

    @Test
    void create_name31Chars_returns400() throws Exception {
        mvc.perform(post("/api/folders").contentType(MediaType.APPLICATION_JSON).content(folderBody("가".repeat(31), "mint")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("폴더 이름은 30자 이하로 입력해 주세요."));
    }

    @Test
    void create_unknownColor_returns400() throws Exception {
        mvc.perform(post("/api/folders").contentType(MediaType.APPLICATION_JSON).content(folderBody("자료구조", "neon")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("요청 형식이 올바르지 않습니다."));
    }

    @Test
    void create_missingColor_returns400() throws Exception {
        mvc.perform(post("/api/folders").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"자료구조\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("폴더 색을 골라 주세요."));
    }

    @Test
    void update_changesNameAndColor() throws Exception {
        long id = createFolder("자료구조", "mint");

        mvc.perform(put("/api/folders/" + id).contentType(MediaType.APPLICATION_JSON).content(folderBody("자료구조(복습)", "pink")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("자료구조(복습)"))
                .andExpect(jsonPath("$.color").value("pink"));
    }

    @Test
    void update_unknown_returns404() throws Exception {
        mvc.perform(put("/api/folders/999999").contentType(MediaType.APPLICATION_JSON).content(folderBody("x", "mint")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("폴더를 찾을 수 없습니다. id=999999"));
    }

    @Test
    void delete_returns204() throws Exception {
        long id = createFolder("자료구조", "mint");

        mvc.perform(delete("/api/folders/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/folders")).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void delete_movesNotesToUnfiled() throws Exception {
        long folderId = createFolder("자료구조", "mint");
        long noteId = createNote("트리 정리", folderId);

        mvc.perform(delete("/api/folders/" + folderId)).andExpect(status().isNoContent());

        mvc.perform(get("/api/notes/" + noteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("트리 정리"))
                .andExpect(jsonPath("$.folderId").doesNotExist());
    }
}
