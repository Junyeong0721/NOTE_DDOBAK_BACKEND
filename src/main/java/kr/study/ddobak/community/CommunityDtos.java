package kr.study.ddobak.community;

import java.time.Instant;
import java.util.List;

/** 커뮤니티 API 응답·요청 모양 */
public final class CommunityDtos {

    private CommunityDtos() {
    }

    /** 목록 카드 (필기 데이터는 빼고 미리보기만) */
    public record PostCard(Long id, String title, String subject, String intro, String summaryLine, String thumbnail,
                           String author, int likeCount, int commentCount, boolean liked, boolean mine, boolean hidden,
                           Instant publishedAt) {
    }

    public record PostPage(List<PostCard> items, boolean hasMore) {
    }

    public record PostDetail(Long id, String title, String subject, String intro, String summary, String content,
                             String author, int likeCount, int commentCount, int copyCount, boolean liked, boolean mine,
                             boolean hidden, boolean reported, Long noteId, Instant publishedAt, Instant updatedAt) {
    }

    public record SubjectCount(String subject, long count) {
    }

    public record LikeResponse(boolean liked, int likeCount) {
    }

    public record CopyResponse(Long noteId) {
    }

    public record CommentRequest(String body) {
    }

    public record CommentResponse(Long id, String author, String body, boolean mine, boolean canDelete, Instant createdAt) {
    }

    public record ReportRequest(String reason) {
    }
}
