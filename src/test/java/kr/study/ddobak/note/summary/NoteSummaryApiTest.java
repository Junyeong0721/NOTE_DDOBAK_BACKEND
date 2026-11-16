package kr.study.ddobak.note.summary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import kr.study.ddobak.note.NoteRepository;
import kr.study.ddobak.support.TestAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

/** 요약 AI를 흉내 내는 클라이언트로 바꿔서 저장·실패·하루 한도(2번)를 본다 */
@SpringBootTest(properties = "ddobak.summary.daily-limit=2")
@AutoConfigureMockMvc
class NoteSummaryApiTest {

    static final String PNG = "data:image/png;base64,"
            + "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==";
    static final String CONTENT = "{\"version\":1,\"pages\":[{\"strokes\":[]}]}";

    static class StubSummaryClient implements SummaryClient {
        final List<List<byte[]>> calls = new ArrayList<>();
        final List<String> titles = new ArrayList<>();
        RuntimeException failure;

        @Override
        public NoteSummaryResult summarize(String currentTitle, List<byte[]> pages) {
            calls.add(pages);
            titles.add(currentTitle);
            if (failure != null) throw failure;
            return new NoteSummaryResult(List.of("극한은 x가 a에 가까워질 때의 값이다.", "좌극한과 우극한이 같아야 극한이 있다."), "미적분 2강 극한");
        }
    }

    @TestConfiguration
    static class StubConfig {
        @Bean
        @Primary
        StubSummaryClient stubSummaryClient() {
            return new StubSummaryClient();
        }
    }

    @Autowired MockMvc baseMvc;
    @Autowired WebApplicationContext ctx;
    @Autowired StubSummaryClient stub;
    @Autowired NoteRepository noteRepository;
    @Autowired JsonMapper jsonMapper;
    MockMvc mvc;
    long noteId;

    @BeforeEach
    void setUp() throws Exception {
        stub.calls.clear();
        stub.titles.clear();
        stub.failure = null;
        mvc = TestAuth.signup(baseMvc, ctx).mvc();
        noteId = createNote(mvc, "제목 없음");
    }

    long createNote(MockMvc client, String title) throws Exception {
        String res = client.perform(post("/api/notes").contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("title", title, "content", CONTENT))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(res, "$.id")).longValue();
    }

    ResultActions summarize(MockMvc client, long id, List<String> pages) throws Exception {
        return client.perform(post("/api/notes/" + id + "/summary").contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("pages", pages))));
    }

    @Test
    void summary_isSaved_andShownInTheNoteAndTheList_withoutChangingUpdatedAt() throws Exception {
        String before = mvc.perform(get("/api/notes/" + noteId)).andReturn().getResponse().getContentAsString();
        String updatedBefore = JsonPath.read(before, "$.updatedAt");

        summarize(mvc, noteId, List.of(PNG, PNG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.length()").value(2))
                .andExpect(jsonPath("$.summary[0]").value("극한은 x가 a에 가까워질 때의 값이다."))
                .andExpect(jsonPath("$.title").value("미적분 2강 극한"))
                .andExpect(jsonPath("$.summarizedAt").isNotEmpty());

        assertThat(stub.calls).hasSize(1);
        assertThat(stub.calls.get(0)).hasSize(2);
        assertThat(stub.titles).containsExactly("제목 없음");

        mvc.perform(get("/api/notes/" + noteId))
                .andExpect(jsonPath("$.summary").value("극한은 x가 a에 가까워질 때의 값이다.\n좌극한과 우극한이 같아야 극한이 있다."))
                .andExpect(jsonPath("$.summarizedAt").isNotEmpty())
                .andExpect(jsonPath("$.title").value("제목 없음")) // 제목 추천은 저장하지 않는다
                .andExpect(jsonPath("$.updatedAt").value(updatedBefore)); // 요약해도 수정 시각은 그대로
        mvc.perform(get("/api/notes"))
                .andExpect(jsonPath("$[0].summary").value("극한은 x가 a에 가까워질 때의 값이다.\n좌극한과 우극한이 같아야 극한이 있다."));
    }

    @Test
    void editingAfterSummary_makesUpdatedAtLater() throws Exception {
        String res = summarize(mvc, noteId, List.of(PNG)).andReturn().getResponse().getContentAsString();
        Instant summarizedAt = Instant.parse(JsonPath.read(res, "$.summarizedAt"));
        Thread.sleep(5);
        String after = mvc.perform(put("/api/notes/" + noteId).contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("title", "제목 없음",
                                "content", "{\"version\":1,\"pages\":[{\"strokes\":[]},{\"strokes\":[]}]}"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(Instant.parse(JsonPath.read(after, "$.updatedAt"))).isAfter(summarizedAt);
        assertThat((String) JsonPath.read(after, "$.summary")).isNotBlank(); // 요약은 남아 있다
    }

    @Test
    void failedSummary_returns503_keepsTheOldOne_andDoesNotUseUpTheLimit() throws Exception {
        summarize(mvc, noteId, List.of(PNG)).andExpect(status().isOk());
        stub.failure = new SummaryFailedException("요약 서버에 연결하지 못했습니다.");
        for (int i = 0; i < 3; i++) {
            summarize(mvc, noteId, List.of(PNG))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.message").value("지금은 요약을 만들 수 없어요. 잠시 뒤 다시 해 주세요."));
        }
        stub.failure = new IllegalStateException("boom");
        summarize(mvc, noteId, List.of(PNG)).andExpect(status().isServiceUnavailable());
        mvc.perform(get("/api/notes/" + noteId)).andExpect(jsonPath("$.summary").isNotEmpty());

        stub.failure = null;
        summarize(mvc, noteId, List.of(PNG)).andExpect(status().isOk()); // 실패는 횟수에서 빠져서 아직 2번째
        summarize(mvc, noteId, List.of(PNG))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("오늘 AI 요약을 다 썼어요. (하루 2번) 내일 다시 해 주세요."));
    }

    @Test
    void badRequests_return400_withoutCallingTheAi() throws Exception {
        summarize(mvc, noteId, List.of()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("요약할 필기가 없어요."));
        summarize(mvc, noteId, Collections.nCopies(11, PNG)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("요약은 10쪽까지 할 수 있어요."));
        summarize(mvc, noteId, List.of("data:image/jpeg;base64,AAAA")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("그림 형식이 올바르지 않습니다."));
        summarize(mvc, noteId, List.of("data:image/png;base64,AAAA")).andExpect(status().isBadRequest());
        assertThat(stub.calls).isEmpty();
    }

    @Test
    void otherAccounts_cannotSummarizeMyNote() throws Exception {
        MockMvc other = TestAuth.signup(baseMvc, ctx).mvc();
        summarize(other, noteId, List.of(PNG)).andExpect(status().isNotFound());
        summarize(baseMvc, noteId, List.of(PNG)).andExpect(status().isUnauthorized());
        assertThat(stub.calls).isEmpty();
        assertThat(noteRepository.findById(noteId).orElseThrow().getSummary()).isNull();
    }
}
