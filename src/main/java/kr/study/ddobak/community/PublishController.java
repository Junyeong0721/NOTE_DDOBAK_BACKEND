package kr.study.ddobak.community;

import kr.study.ddobak.auth.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 내 노트의 커뮤니티 공개 설정 */
@RestController
@RequestMapping("/api/notes/{noteId}/publish")
@RequiredArgsConstructor
public class PublishController {

    private final PublishService publishService;

    @GetMapping
    public PublishStatus status(@LoginUser Long userId, @PathVariable Long noteId) {
        return publishService.status(userId, noteId);
    }

    @PutMapping
    public PublishStatus publish(@LoginUser Long userId, @PathVariable Long noteId, @RequestBody PublishRequest req) {
        return publishService.publish(userId, noteId, req);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unpublish(@LoginUser Long userId, @PathVariable Long noteId) {
        publishService.unpublish(userId, noteId);
    }
}
