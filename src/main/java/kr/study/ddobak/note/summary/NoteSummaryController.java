package kr.study.ddobak.note.summary;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import kr.study.ddobak.auth.LoginUser;
import kr.study.ddobak.common.PngDataUrl;
import kr.study.ddobak.note.InvalidContentException;
import kr.study.ddobak.note.NoteResponse;
import kr.study.ddobak.note.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** AI 노트 요약: 페이지 그림 → Gemini → 요약 저장 + 제목 추천 */
@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteSummaryController {

    static final int MAX_PAGES = 10;
    static final int MAX_PAGE_BYTES = 2 * 1024 * 1024;

    private final NoteService noteService;
    private final SummaryClient summaryClient;
    private final SummaryUsageLimiter limiter;

    @PostMapping("/{id}/summary")
    public NoteSummaryResponse summarize(@LoginUser Long userId, @PathVariable Long id, @RequestBody NoteSummaryRequest req) {
        NoteResponse note = noteService.get(userId, id); // 남의 노트면 404
        List<String> pages = req == null || req.pages() == null ? List.of() : req.pages();
        if (pages.isEmpty()) {
            throw new InvalidContentException("요약할 필기가 없어요.");
        }
        if (pages.size() > MAX_PAGES) {
            throw new InvalidContentException("요약은 " + MAX_PAGES + "쪽까지 할 수 있어요.");
        }
        List<byte[]> pngs = new ArrayList<>();
        for (String page : pages) {
            try {
                pngs.add(PngDataUrl.decode(page, MAX_PAGE_BYTES));
            } catch (IllegalArgumentException e) {
                throw new InvalidContentException(e.getMessage());
            }
        }
        if (!limiter.tryAcquire(userId)) {
            throw new SummaryLimitExceededException(limiter.dailyLimit());
        }
        NoteSummaryResult result;
        try {
            result = summaryClient.summarize(note.title(), pngs);
        } catch (RuntimeException e) {
            limiter.release(userId); // 받지 못한 요약은 오늘 횟수에서 빼지 않는다
            throw e instanceof SummaryFailedException ? e : new SummaryFailedException(e.getMessage());
        }
        Instant at = noteService.saveSummary(userId, id, String.join("\n", result.lines()));
        return new NoteSummaryResponse(result.lines(), result.title(), at);
    }
}
