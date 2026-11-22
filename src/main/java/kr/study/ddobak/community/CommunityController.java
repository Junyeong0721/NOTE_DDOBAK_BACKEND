package kr.study.ddobak.community;

import java.util.List;
import kr.study.ddobak.auth.LoginUser;
import kr.study.ddobak.community.CommunityDtos.CommentRequest;
import kr.study.ddobak.community.CommunityDtos.CommentResponse;
import kr.study.ddobak.community.CommunityDtos.CopyResponse;
import kr.study.ddobak.community.CommunityDtos.LikeResponse;
import kr.study.ddobak.community.CommunityDtos.PostDetail;
import kr.study.ddobak.community.CommunityDtos.PostPage;
import kr.study.ddobak.community.CommunityDtos.ReportRequest;
import kr.study.ddobak.community.CommunityDtos.SubjectCount;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 노트 공유 커뮤니티 (또박 회원만) */
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    @GetMapping
    public PostPage list(@LoginUser Long me,
                         @RequestParam(defaultValue = "latest") String sort,
                         @RequestParam(required = false) String subject,
                         @RequestParam(required = false) String q,
                         @RequestParam(defaultValue = "0") int page) {
        return communityService.list(me, sort, subject, q, page);
    }

    @GetMapping("/subjects")
    public List<SubjectCount> subjects(@LoginUser Long me) {
        return communityService.subjects();
    }

    @GetMapping("/{postId}")
    public PostDetail get(@LoginUser Long me, @PathVariable Long postId) {
        return communityService.get(me, postId);
    }

    @PutMapping("/{postId}/like")
    public LikeResponse like(@LoginUser Long me, @PathVariable Long postId) {
        return communityService.like(me, postId, true);
    }

    @DeleteMapping("/{postId}/like")
    public LikeResponse unlike(@LoginUser Long me, @PathVariable Long postId) {
        return communityService.like(me, postId, false);
    }

    @PostMapping("/{postId}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    public CopyResponse copy(@LoginUser Long me, @PathVariable Long postId) {
        return communityService.copy(me, postId);
    }

    @GetMapping("/{postId}/comments")
    public List<CommentResponse> comments(@LoginUser Long me, @PathVariable Long postId) {
        return communityService.comments(me, postId);
    }

    @PostMapping("/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(@LoginUser Long me, @PathVariable Long postId, @RequestBody CommentRequest req) {
        return communityService.addComment(me, postId, req.body());
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@LoginUser Long me, @PathVariable Long postId, @PathVariable Long commentId) {
        communityService.deleteComment(me, postId, commentId);
    }

    @PostMapping("/{postId}/report")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void report(@LoginUser Long me, @PathVariable Long postId, @RequestBody ReportRequest req) {
        communityService.report(me, postId, req.reason());
    }
}
